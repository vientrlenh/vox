package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.QuotaDebtConfigPort;
import com.sep.vox.application.port.output.QuotaUsageWarningConfigPort;
import com.sep.vox.application.response.input.subscription.ConsumeQuotaResponse;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolAiSpendEntry;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolAiSpendEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Deducts AI cost from the quota included in the plan and, once that quota is exhausted, continues deducting from the school's self-funded balance.
 *
 * <p>DEBT = {@code school_balances.balance_vnd < 0}, NO LONGER
 * {@code used_amount_vnd > total_allocated_amount_vnd}. This is the core change of the refactor: the two columns
 * of the quota record now only express "how much the plan grants / how much of it has been spent", so used must NEVER
 * exceed total again -- the excess has become a deduction entry on the ledger. See V2 section 6
 * ("the negative part here IS the debt, replacing the old condition used_quantity > total_allocated").
 *
 * <p>Only the EXCESS goes into school_balance_entries. The part within the quota is already counted in
 * used_amount_vnd; recording it again in the ledger counts the same money twice and breaks the invariant
 * {@code SUM(entries.amount_vnd) = balance_vnd} (V2 section 7).
 *
 * <p>Internal service-to-service (called from the grading / speaking-practice flow), not end-user-facing -- it does not
 * check school scoping through UserContextPort here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumeQuotaService {

    private final SchoolSubscriptionQuotaRecordRepository schoolSubscriptionQuotaRecordRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository schoolSubscriptionQuotaUserAllocationRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolDebtNotificationService schoolDebtNotificationService;
    private final QuotaDebtConfigPort quotaDebtConfig;
    private final SchoolQuotaUsageNotificationService schoolQuotaUsageNotificationService;
    private final QuotaUsageWarningConfigPort quotaUsageWarningConfig;
    private final SchoolAiSpendEntryRepository schoolAiSpendEntryRepository;

    /**
     * Deducts the cost of an ASSESSMENT session. Records the FULL expense even if it exceeds the quota: the real AI cost has already
     * been incurred, and refusing here would only lose track of the money without getting it back.
     *
     * <p>Does not take {@code quotaType}: an assessment session always deducts from the ASSESSMENT wallet. Making it a parameter would open the door
     * to writing an ASSESSMENT deduction carrying the id of a speaking practice session (or the other way round), i.e. a self-contradicting
     * ledger row that no constraint at the Java layer can block.
     *
     * @param amountVnd  cost converted to VND -- NOT cost_usd
     * @param costUsd    the original cost charged by the provider, kept for reconciliation against ai_usage_records
     * @param fxRateUsed the exchange rate used to get amountVnd, recorded because the rate changes daily
     */
    @Transactional
    public ConsumeQuotaResponse consumeAssessmentAllowingDebt(
            UUID subscriptionId, UUID assessmentSessionId, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        return consumeAllowingDebt(subscriptionId, assessmentSessionId, null, QuotaType.ASSESSMENT,
            amountVnd, costUsd, fxRateUsed, userId);
    }

    /**
     * Deducts the cost of one SPEAKING PRACTICE session -- always the PRACTICE quota.
     *
     * <p>Spending into the balance is allowed here too, as on the assessment path, for the same reason: by the time this is
     * called Azure has already charged (turn.turnCostUsd is the REAL cost of the turn just spoken, computed by Python
     * synchronously in the submit_turn request). Blocking here would not hold back a single dong, it would only make that
     * spend vanish from the books and VOX would silently absorb it.
     *
     * <p>Whether the student may keep speaking is a DIFFERENT question, answered by
     * {@link ConsumeQuotaResponse#fundsExhausted()} and not by an exception -- see
     * SubmitTurnResultDto.quotaExhausted.
     */
    @Transactional
    public ConsumeQuotaResponse consumePracticeAllowingDebt(
            UUID subscriptionId, UUID practiceSessionId, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        return consumeAllowingDebt(subscriptionId, null, practiceSessionId, QuotaType.PRACTICE,
            amountVnd, costUsd, fxRateUsed, userId);
    }

    private ConsumeQuotaResponse consumeAllowingDebt(
            UUID subscriptionId, UUID assessmentSessionId, UUID practiceSessionId, QuotaType quotaType,
            BigDecimal amountVnd, BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        SchoolSubscriptionQuotaRecord quota = requireQuota(subscriptionId, quotaType);

        // tryConsume is a conditional UPDATE, so it is itself the guard -- no read-then-compare in Java.
        // Passing = this amount fits within the quota: no balance is touched and no ledger entry is created.
        ChargeSplit split;
        if (schoolSubscriptionQuotaRecordRepository.tryConsume(quota.getId(), amountVnd)) {
            checkUsageWarningTransition(subscriptionId, quotaType, quota.getTotalAllocatedAmountVnd(),
                quota.getUsedAmountVnd(), quota.getUsedAmountVnd().add(amountVnd), Instant.now());
            split = new ChargeSplit(amountVnd, BigDecimal.ZERO, currentBalanceVnd(subscriptionId), false);
        } else {
            split = chargeOverage(subscriptionId, assessmentSessionId, practiceSessionId, quotaType, quota,
                amountVnd, costUsd, fxRateUsed);
        }

        consumeUserAllocation(subscriptionId, quotaType, userId, amountVnd);
        recordSpend(subscriptionId, assessmentSessionId, practiceSessionId, quotaType, amountVnd, userId);
        return buildResponse(subscriptionId, quota.getId(), split);
    }

    /**
     * Writes to the school's AI cost ledger -- the FULL amount, not just the part over the quota.
     *
     * <p>Placed here, AFTER both branches, because this is the only place that knows the school, quota type, spender,
     * amount and session all at once. If the caller wrote it, every new source of spending would be another chance to forget,
     * and the speaking-practice path being absent from every report for so long is the consequence of exactly that kind of
     * forgetting (see V10).
     *
     * <p>DIFFERENT from {@code school_balance_entries} in that it records the part within the quota too: that one is
     * the LEDGER of the BALANCE so it only takes the excess, while this one answers "how much has the school spent".
     * Adding the two together counts the same money twice.
     *
     * <p>{@code userId} null is an ANSWER -- a centralized assessment belongs to nobody's spending cap.
     */
    private void recordSpend(UUID subscriptionId, UUID assessmentSessionId, UUID practiceSessionId,
            QuotaType quotaType, BigDecimal amountVnd, UUID userId) {
        // The constraint chk_..._amount_positive rejects a 0-dong row, and a 0-dong deduction is not an
        // event worth recording either -- block it here instead of letting the DB throw in the middle of grading.
        if (amountVnd == null || amountVnd.signum() <= 0) {
            return;
        }

        UUID schoolId = requireSchoolId(subscriptionId);
        Instant now = Instant.now();
        schoolAiSpendEntryRepository.save(assessmentSessionId != null
            ? SchoolAiSpendEntry.forAssessment(schoolId, subscriptionId, assessmentSessionId, userId, amountVnd, now)
            : SchoolAiSpendEntry.forPractice(
                schoolId, subscriptionId, practiceSessionId, userId, amountVnd, now));
    }

    /**
     * Splits the expense in two: the remaining quota is spent right down to its last dong, the overage is deducted from the
     * self-topped-up wallet.
     *
     * <p>Locks the balance row FIRST and only then re-reads the quota: that is the only write path according to the javadoc of
     * {@link SchoolBalanceRepository} (lock row -> compute in Java -> save + write entry, same transaction),
     * and that same lock also serializes every debt write of the same school.
     *
     * <p>The balance lock does NOT lock the quota row, so {@code remainingVnd} can still drift if an
     * addAllocation (renewal/top-up) cuts in. That drift is harmless money-wise: the total deducted always equals
     * {@code amountVnd} however it is split, only the boundary between "deduct from quota" and "deduct from wallet"
     * shifts. The two clamps below keep that drift from ever pushing used above total.
     */
    private ChargeSplit chargeOverage(UUID subscriptionId, UUID assessmentSessionId, UUID practiceSessionId,
            QuotaType quotaType, SchoolSubscriptionQuotaRecord quota, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed) {
        UUID schoolId = requireSchoolId(subscriptionId);
        Instant now = Instant.now();
        SchoolBalance balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(schoolId, now);
        SchoolSubscriptionQuotaRecord current = requireQuota(subscriptionId, quotaType);
        BigDecimal remainingVnd = remainingQuotaWithin(current, amountVnd);
        BigDecimal overageVnd = amountVnd.subtract(remainingVnd);
        consumeRemainingQuota(quota.getId(), subscriptionId, quotaType, current, remainingVnd, now);

        // An addAllocation (renewal/top-up) cut in between the failed tryConsume and the re-read above, so the
        // expense fits within the quota after all. With no overage, do NOT write a ledger entry:
        // chk_school_balance_entries_overage_traceable demands amount_vnd < 0, so a 0 dong row would both violate the
        // constraint and mean nothing on the statement.
        if (overageVnd.signum() <= 0) {
            return new ChargeSplit(remainingVnd, BigDecimal.ZERO, balance.getBalanceVnd(), false);
        }
        OverageCharge charge = new OverageCharge(subscriptionId, schoolId, quotaType, assessmentSessionId,
            practiceSessionId, amountVnd, costUsd, fxRateUsed, current.getTotalAllocatedAmountVnd(), overageVnd, now);
        return chargeOverageToBalance(charge, balance, remainingVnd);
    }

    private BigDecimal remainingQuotaWithin(SchoolSubscriptionQuotaRecord current, BigDecimal amountVnd) {
        return current.getTotalAllocatedAmountVnd()
            .subtract(current.getUsedAmountVnd())
            .max(BigDecimal.ZERO)
            .min(amountVnd);
    }

    private void consumeRemainingQuota(UUID quotaId, UUID subscriptionId, QuotaType quotaType,
            SchoolSubscriptionQuotaRecord current, BigDecimal remainingVnd, Instant now) {
        if (remainingVnd.signum() > 0) {
            schoolSubscriptionQuotaRecordRepository.addUsage(quotaId, remainingVnd);
            checkUsageWarningTransition(subscriptionId, quotaType, current.getTotalAllocatedAmountVnd(),
                current.getUsedAmountVnd(), current.getUsedAmountVnd().add(remainingVnd), now);
        }
    }

    private ChargeSplit chargeOverageToBalance(OverageCharge charge, SchoolBalance balance, BigDecimal remainingVnd) {
        BigDecimal balanceBeforeVnd = balance.getBalanceVnd();
        BigDecimal balanceAfterVnd = writeOverageLedgerEntry(charge, balance);

        // Pass BOTH sources, exactly like when writing the ledger entry right above. The previous version only passed assessmentSessionId
        // while practiceSessionId was within reach, so every CAP_EXCEEDED row caused by speaking practice
        // lost its trace.
        checkDebtCapTransition(charge.subscriptionId(), charge.schoolId(), charge.quotaType(),
            charge.assessmentSessionId(), charge.practiceSessionId(), charge.overageVnd(),
            charge.totalAllocatedVnd(), balanceBeforeVnd, balanceAfterVnd, charge.now());

        // crossedIntoDebt takes BEFORE/AFTER of the same row while holding the lock -- not two separate
        // reads, so it cannot disagree with what really just happened above.
        boolean crossedIntoDebt = balanceBeforeVnd.signum() >= 0 && balanceAfterVnd.signum() < 0;
        if (crossedIntoDebt) {
            publishSchoolLockedDueToDebt(charge, balanceAfterVnd);
        }

        return new ChargeSplit(remainingVnd, charge.overageVnd(), balanceAfterVnd, crossedIntoDebt);
    }

    private BigDecimal writeOverageLedgerEntry(OverageCharge charge, SchoolBalance balance) {
        BigDecimal balanceAfterVnd = balance.apply(charge.overageVnd().negate(), charge.now());
        schoolBalanceRepository.save(balance);
        schoolBalanceEntryRepository.save(charge.assessmentSessionId() != null
            ? SchoolBalanceEntry.forAssessmentOverageCharge(
                charge.schoolId(), charge.subscriptionId(), charge.assessmentSessionId(), charge.quotaType(),
                charge.overageVnd(), balanceAfterVnd, charge.costUsd(), charge.fxRateUsed(), charge.now())
            : SchoolBalanceEntry.forPracticeOverageCharge(
                charge.schoolId(), charge.subscriptionId(), charge.practiceSessionId(), charge.quotaType(),
                charge.overageVnd(), balanceAfterVnd, charge.costUsd(), charge.fxRateUsed(), charge.now()));
        return balanceAfterVnd;
    }

    // Announces "the school has just been locked" RIGHT HERE rather than leaving it to the caller.
    //
    // This used to live in CompleteAssessmentSessionGradingUseCase, i.e. only the ASSESSMENT GRADING path had it --
    // speaking practice pushes a school into debt just the same without producing any LOCKED row or sending an email to
    // anyone. The school was still really locked (the guard derives from the sign of the balance, regardless of which source caused it), so
    // the principal lost the right to open assessment sessions with nowhere to explain why.
    //
    // Placed here, every deduction source, present and future, gets announced, and the event row commits in the
    // SAME transaction as the ledger entry that caused it -- there is no window where one gets recorded and the
    // other does not.
    private void publishSchoolLockedDueToDebt(OverageCharge charge, BigDecimal balanceAfterVnd) {
        schoolDebtNotificationService.publishSchoolLockedDueToDebt(
            charge.subscriptionId(), charge.schoolId(), charge.quotaType(), charge.assessmentSessionId(),
            charge.practiceSessionId(), charge.amountVnd(), charge.totalAllocatedVnd(),
            balanceAfterVnd.negate().max(BigDecimal.ZERO), charge.now()
        );
    }

    /**
     * The PERSONAL spending cap that the school divides internally (teachers creating class tests, students practising
     * speaking). No allocation row = the school did not divide anything for anyone = no personal blocking.
     *
     * <p>This is a LIMIT, not a balance: it holds no money and never creates a ledger entry
     * -- exceeding the personal cap only makes used exceed allocated on that very allocation row, without touching the
     * school's balance. See QuotaType.
     *
     * <p>It only RECORDS, it does NOT block -- and there is no longer an allowDebt parameter to choose between two styles. Both
     * call paths (assessment and speaking practice) run AFTER Azure has charged, so "reject for exceeding the
     * personal cap" at this step would not hold back a single dong, it would only make the real spend vanish from the
     * counter of the very person who spent it. BLOCKING by the personal cap happens at the gate before money is spent:
     * ClassTestTokenQuotaGuardService.requireWithinUserAllocation for class tests, and
     * SchoolSubscriptionRepository.findPracticeSpendableFundsVnd for speaking practice.
     */
    private void consumeUserAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal amountVnd) {
        if (userId == null) {
            return;
        }
        schoolSubscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, quotaType, userId)
            .ifPresent(allocation ->
                schoolSubscriptionQuotaUserAllocationRepository.addUsage(allocation.getId(), amountVnd));
    }

    /**
     * Only WARNS (log + SYSTEM_ADMIN notification), does NOT block -- the real cost was already fully recorded above. The purpose is
     * to detect early if the AI-cost measuring pipeline has a bug that makes debt grow abnormally, not to
     * cap the maximum debt.
     *
     * <p>Compares the balance BEFORE/AFTER so that it reports exactly once, when the balance CROSSES from below the cap to above it. Both
     * numbers come straight from {@link SchoolBalance#apply}, so no second DB read is needed -- unlike
     * the old version, which had to fetch before/after around a bulk update and therefore needed {@code clearAutomatically}.
     */
    private void checkDebtCapTransition(UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            UUID assessmentSessionId, UUID practiceSessionId, BigDecimal overageVnd, BigDecimal totalAllocatedVnd,
            BigDecimal balanceBeforeVnd, BigDecimal balanceAfterVnd, Instant now) {
        BigDecimal cap = totalAllocatedVnd.multiply(quotaDebtConfig.capRatio());
        BigDecimal debtBeforeVnd = balanceBeforeVnd.negate().max(BigDecimal.ZERO);
        BigDecimal debtAfterVnd = balanceAfterVnd.negate().max(BigDecimal.ZERO);

        if (debtAfterVnd.compareTo(cap) <= 0 || debtBeforeVnd.compareTo(cap) > 0) {
            return;
        }

        log.warn(
            "Debt exceeded the warning cap: subscriptionId={} quotaType={} assessmentSessionId={} practiceSessionId={} debtVnd={} capVnd={}",
            subscriptionId, quotaType, assessmentSessionId, practiceSessionId, debtAfterVnd, cap
        );

        schoolDebtNotificationService.publishDebtCapExceeded(
            subscriptionId, schoolId, quotaType, assessmentSessionId, practiceSessionId, overageVnd,
            totalAllocatedVnd, totalAllocatedVnd.add(debtAfterVnd), debtAfterVnd, cap, now
        );
    }

    /**
     * Only an EARLY WARNING (log + SCHOOL_ADMIN notification), does NOT block -- the quota is NOT yet exhausted, this is only a
     * heads-up so the school can consider renewing/topping up before it is counted into debt (see
     * {@link #checkDebtCapTransition}).
     *
     * <p>Compares {@code used/total} BEFORE/AFTER so that it reports exactly once, when it CROSSES from below the threshold to
     * above it -- the same crossing-detection technique as checkDebtCapTransition, so it "resets" by itself
     * when the school renews/tops up, which raises {@code totalAllocatedVnd} and drops the ratio below the threshold.
     */
    private void checkUsageWarningTransition(UUID subscriptionId, QuotaType quotaType,
            BigDecimal totalAllocatedVnd, BigDecimal usedBeforeVnd, BigDecimal usedAfterVnd, Instant now) {
        if (totalAllocatedVnd.signum() <= 0) {
            return;
        }

        BigDecimal threshold = totalAllocatedVnd.multiply(quotaUsageWarningConfig.warningRatio());
        if (usedAfterVnd.compareTo(threshold) < 0 || usedBeforeVnd.compareTo(threshold) >= 0) {
            return;
        }

        log.info(
            "Quota exceeded the early-warning threshold: subscriptionId={} quotaType={} usedVnd={} totalVnd={}",
            subscriptionId, quotaType, usedAfterVnd, totalAllocatedVnd
        );

        schoolQuotaUsageNotificationService.publishQuotaUsageWarning(
            subscriptionId, requireSchoolId(subscriptionId), quotaType, totalAllocatedVnd, usedAfterVnd, now
        );
    }

    /** Everything about one overage deduction that the balance-side steps need; {@code totalAllocatedVnd} is the quota total read under the balance lock. */
    private record OverageCharge(
        UUID subscriptionId,
        UUID schoolId,
        QuotaType quotaType,
        @Nullable UUID assessmentSessionId,
        @Nullable UUID practiceSessionId,
        BigDecimal amountVnd,
        BigDecimal costUsd,
        BigDecimal fxRateUsed,
        BigDecimal totalAllocatedVnd,
        BigDecimal overageVnd,
        Instant now
    ) {
    }

    /** The result of splitting a spend between the quota record and the balance -- an internal part of {@link ConsumeQuotaResponse}. */
    private record ChargeSplit(
        BigDecimal chargedToQuotaVnd,
        BigDecimal chargedToBalanceVnd,
        BigDecimal balanceAfterVnd,
        boolean crossedIntoDebt
    ) {
    }

    /**
     * Re-reads the quota record AFTER the deduction and merges it with the part computed while the balance lock was still held.
     *
     * <p>{@code fundsExhausted} must look at BOTH pockets: with the quota record exhausted but money left in the balance the school can still
     * spend, so looking at one side only would close the speaking-practice session earlier than it should.
     */
    private ConsumeQuotaResponse buildResponse(UUID subscriptionId, UUID quotaId, ChargeSplit split) {
        SchoolSubscriptionQuotaRecord quota = schoolSubscriptionQuotaRecordRepository.findById(quotaId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription quota not found"));

        BigDecimal quotaRemainingVnd = quota.getTotalAllocatedAmountVnd().subtract(quota.getUsedAmountVnd());
        boolean fundsExhausted = quotaRemainingVnd.signum() <= 0 && split.balanceAfterVnd().signum() <= 0;

        return new ConsumeQuotaResponse(
            SchoolSubscriptionQuotaRecordDto.toDto(quota),
            split.chargedToQuotaVnd(),
            split.chargedToBalanceVnd(),
            split.balanceAfterVnd(),
            split.crossedIntoDebt(),
            fundsExhausted
        );
    }

    private SchoolSubscriptionQuotaRecord requireQuota(UUID subscriptionId, QuotaType quotaType) {
        return schoolSubscriptionQuotaRecordRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription quota not found"));
    }

    private UUID requireSchoolId(UUID subscriptionId) {
        return schoolSubscriptionRepository.findById(subscriptionId)
            .map(s -> s.getSchoolId())
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
    }

    /** READ-ONLY copy -- use only on the path that does NOT write the balance (see the SchoolBalanceRepository Javadoc). */
    private BigDecimal currentBalanceVnd(UUID subscriptionId) {
        return schoolBalanceRepository.findBySchoolId(requireSchoolId(subscriptionId))
            .map(b -> b.getBalanceVnd())
            .orElse(BigDecimal.ZERO);
    }
}
