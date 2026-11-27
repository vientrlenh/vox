package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.FundQuotaFromBalanceCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.QuotaFundingResponse;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Moves money from the school's SELF-FUNDED balance to the QUOTA pool of one quota type.
 *
 * <p><b>Why the money goes into the SCHOOL's pool and not into one person's spending cap.</b> The alternative
 * "deduct the balance by whatever amount is approved" deducts money TWICE: the allocation row holds no money, so
 * when that person really spends, {@code tryConsume} still fails (the quota pool is still empty) and
 * {@code ConsumeQuotaService.chargeOverage} deducts the balance again. The balance loses 800k for 300k of real AI
 * cost, while {@code school_ai_spend_entries} records only 300k -- two ledgers telling two different numbers.
 * Details in V12.
 *
 * <p>With the money in the quota pool, {@code tryConsume} sees room, {@code chargeOverage} does not run, and NOT
 * ONE line of ConsumeQuotaService has to change. What is left unspent stays in the school's pool to be divided
 * among others, instead of being stuck under the name of a student who never comes to practise.
 *
 * <p><b>ONE WAY, AND CURRENTLY IRREVERSIBLE.</b> There is no path back to the balance, and no transfer to the
 * other quota type either. A wrong choice of pool has <b>no fix</b> -- and do not think an ADJUSTMENT row is one:
 * it only ADDS money back to the balance, while every write to school_subscription_quota_records
 * ({@code addAllocation}, {@code addFundingFromBalance}) only knows how to add, and none of them can lower
 * total_allocated_amount_vnd/funded_from_balance_vnd. Doing it would let the school take the money back and also
 * keep the inflated quota pool: money out of thin air.
 *
 * <p>A real undo has to be a PAIRED operation in the same transaction -- the balance credit entry GOES TOGETHER
 * with a command that lowers the quota pool, clamped at the spent amount so funded never drops below 0 and total
 * never below used. That path does not exist yet, so the only line of defence is the UI: it must say loudly which
 * pool it is before the user confirms.
 *
 * <p><b>The order inside the method is mandatory, not arbitrary</b> -- see the Javadoc of
 * {@link SchoolBalanceRepository}: LOCK the balance row first, compute in Java, then save + write the entry, all
 * in the SAME transaction. Reading with {@code findBySchoolId} and then subtracting would let two administrators
 * who click at the same time both see 2M and both spend it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FundQuotaFromBalanceUseCase implements IUseCase<FundQuotaFromBalanceCommand, QuotaFundingResponse> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    @Override
    @Transactional
    public QuotaFundingResponse execute(FundQuotaFromBalanceCommand input) {
        requireSchoolAdminAccess(input.schoolId());

        QuotaType quotaType = input.quotaType();
        BigDecimal amountVnd = requirePositiveAmount(input.amountVnd());
        UUID actorId = userContextPort.getCurrentAuthenticatedUserId();

        SchoolSubscription subscription = requireActiveSubscription(input.schoolId());
        SchoolSubscriptionQuotaRecord pool = requirePool(subscription.getId(), quotaType);
        requireSchoolNotInDebt(input.schoolId());

        BigDecimal balanceAfterVnd = withdrawFromBalance(input, subscription, amountVnd, actorId);

        // After the entry, and in the same transaction: the money leaving the wallet and the money entering the quota
        // wallet live or die together. One statement adds both total and funded -- see addFundingFromBalance.
        quotaRecordRepository.addFundingFromBalance(pool.getId(), amountVnd);

        log.info("School {} funded {} VND from the school balance into quota {} (period {}), remaining balance {} VND, performed by {}",
            input.schoolId(), amountVnd, quotaType, subscription.getId(), balanceAfterVnd, actorId);

        return fundingResponse(pool.getId(), amountVnd, balanceAfterVnd);
    }

    private SchoolSubscription requireActiveSubscription(UUID schoolId) {
        return schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("The school has no active subscription"));
    }

    private SchoolSubscriptionQuotaRecord requirePool(UUID subscriptionId, QuotaType quotaType) {
        return quotaRecordRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription quota not found"));
    }

    // A school in debt is blocked here, BEFORE looking at the balance -- debt means a negative balance, so the "enough
    // money?" comparison in withdrawFromBalance would also reject, but with the sentence "insufficient balance" instead of
    // one that names the real problem. Same reason ClassTestTokenQuotaGuardService clamps a negative balance to 0 and lets
    // SchoolSubscriptionDebtGuardService answer separately: reporting the wrong reason only points to the wrong remedy.
    private void requireSchoolNotInDebt(UUID schoolId) {
        schoolSubscriptionDebtGuardService.requireSchoolNotLocked(schoolId);
    }

    private BigDecimal withdrawFromBalance(
            FundQuotaFromBalanceCommand input, SchoolSubscription subscription, BigDecimal amountVnd, UUID actorId) {
        Instant now = Instant.now();
        // Lock the balance row. Everything below reads/writes the record holding this lock.
        SchoolBalance balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(input.schoolId(), now);

        if (balance.getBalanceVnd().compareTo(amountVnd) < 0) {
            throw new IllegalArgumentException(
                "Insufficient school balance: needs " + amountVnd + " VND but only "
                    + balance.getBalanceVnd() + " VND left");
        }

        // balanceAfterVnd comes from apply() itself rather than subtracting again here: the entry and the aggregate
        // balance must be one number, and recomputing opens exactly the door for them to drift apart (see
        // SchoolBalance.apply).
        BigDecimal balanceAfterVnd = balance.apply(amountVnd.negate(), now);
        schoolBalanceRepository.save(balance);
        schoolBalanceEntryRepository.save(SchoolBalanceEntry.forQuotaFunding(
            input.schoolId(), subscription.getId(), input.quotaType(), amountVnd, balanceAfterVnd,
            actorId, blankToNull(input.reason()), now));
        return balanceAfterVnd;
    }

    private QuotaFundingResponse fundingResponse(UUID poolId, BigDecimal amountVnd, BigDecimal balanceAfterVnd) {
        SchoolSubscriptionQuotaRecord funded = quotaRecordRepository.findById(poolId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription quota not found"));
        return new QuotaFundingResponse(
            SchoolSubscriptionQuotaRecordDto.toDto(funded), amountVnd, balanceAfterVnd);
    }

    /**
     * Only the administrator of THAT school (or a System Admin). The same check as
     * {@code DistributeQuotaToUsersService.requireSchoolAdminAccess} -- the role was already enforced by
     * @PreAuthorize on the controller; what remains here is stopping a school admin from acting on another school.
     */
    private void requireSchoolAdminAccess(UUID schoolId) {
        if (schoolId == null) {
            throw new IllegalArgumentException("School ID is missing");
        }
        if (!userContextPort.isSystemAdmin() && !schoolId.equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    /**
     * Rejects 0 and negative numbers. A negative number would be a path that ADDS money to the balance with no order
     * behind it (V2: "no money from thin air"), and 0 would produce an entry that violates
     * chk_school_balance_entries_quota_funding_traceable -- rejected here so the error comes out as a readable
     * message instead of a DataIntegrityViolationException.
     */
    private static BigDecimal requirePositiveAmount(BigDecimal amountVnd) {
        if (amountVnd == null || amountVnd.signum() <= 0) {
            throw new IllegalArgumentException("The amount to fund the quota must be greater than 0");
        }
        return amountVnd;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
