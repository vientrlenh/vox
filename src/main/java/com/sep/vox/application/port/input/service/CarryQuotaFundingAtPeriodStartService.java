package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.metering.QuotaType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

/**
 * Carries the unspent self-funded money of the old period into the new one, at EXACTLY the moment the new period starts running.
 *
 * <p><b>Why not do it when the order is settled.</b> {@code OrderSettlementService.seedQuotaRecords} still
 * carries it over immediately when the two milestones coincide (a renewal after the old period has expired -- the most common path).
 * But with an EARLY renewal the new period starts at the old period's endDate, sometimes a whole month after the payment, and
 * in between the OLD period is still the one in force: it can still be spent from and topped up. The figure fixed at
 * payment time is therefore wrong in both directions -- if the school spends the rest, those 5M get spent TWICE;
 * if the school tops up, the new money evaporates at the boundary. See V13.
 *
 * <p><b>Why ADD instead of ASSIGN.</b> The new period may already have started and the school may have topped up that very quota record
 * before the job got to run ({@code FundQuotaFromBalanceUseCase} adds to both total and funded). An assignment
 * "funded = unspent part of the old period" would erase exactly the money they just put in. {@code addFundingFromBalance}
 * adds and keeps everything already there.
 *
 * <p><b>Why exactly once.</b> Adding twice doubles the money, so the carry marker
 * ({@code carry_funding_from_subscription_id}) is cleared right after the addition, in the SAME transaction. A re-run of the
 * job finds nothing to do; a late job only makes the money appear late, it never makes it wrong.
 *
 * <p>No ledger entry is created here and that is correct: this money already LEFT the self-funded balance when it was topped up, with one
 * QUOTA_FUNDING row. Here it only moves on from the old period's quota record to the new period's quota record, without touching
 * {@code school_balances}, so the invariant SUM(entries) = balance_vnd is not involved.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarryQuotaFundingAtPeriodStartService {

    private final SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;

    /**
     * @return the number of quota records that received carried money. 0 is the normal state -- only an early renewal creates a marker.
     */
    @Transactional
    public int carryDueFunding(Instant now) {
        List<SchoolSubscriptionQuotaRecord> due = quotaRecordRepository.findDueFundingCarries(now);
        if (due.isEmpty()) {
            return 0;
        }

        int carriedCount = 0;
        // Group by SOURCE period: each group only needs to read that period's quota records once, and the group is also the unit
        // for detecting quota types left behind (see reportStrandedTypes).
        Map<UUID, List<SchoolSubscriptionQuotaRecord>> bySource = due.stream()
            .collect(Collectors.groupingBy(record -> record.getCarryFundingFromSubscriptionId()));

        for (Map.Entry<UUID, List<SchoolSubscriptionQuotaRecord>> group : bySource.entrySet()) {
            List<SchoolSubscriptionQuotaRecord> sourcePools = quotaRecordRepository.findBySchoolSubscriptionId(group.getKey());
            for (SchoolSubscriptionQuotaRecord target : group.getValue()) {
                if (carryOne(target, sourcePools, group.getKey())) {
                    carriedCount++;
                }
            }
            reportStrandedTypes(sourcePools, group.getValue(), group.getKey());
        }

        return carriedCount;
    }

    private boolean carryOne(SchoolSubscriptionQuotaRecord target,
            List<SchoolSubscriptionQuotaRecord> sourcePools, UUID sourceSubscriptionId) {
        BigDecimal unspentVnd = sourcePools.stream()
            .filter(pool -> pool.getQuotaType() == target.getQuotaType())
            .findFirst()
            .map(record -> record.unspentFundedVnd())
            .orElse(BigDecimal.ZERO);

        // Clear the marker even when there is no money to carry: a lingering marker means the job re-reads it every hour, forever.
        if (unspentVnd.signum() <= 0) {
            quotaRecordRepository.clearFundingCarry(target.getId());
            return false;
        }

        quotaRecordRepository.addFundingFromBalance(target.getId(), unspentVnd);
        quotaRecordRepository.clearFundingCarry(target.getId());

        log.info("Period {} started: carrying {} VND of unspent self-funded money of quota {} over from period {}",
            target.getSchoolSubscriptionId(), unspentVnd, target.getQuotaType(), sourceSubscriptionId);
        return true;
    }

    /**
     * The source period still has self-funded money in a quota type that the new period does NOT have -- the new plan no longer has that quota type.
     *
     * <p>No marker is set for a quota type that does not exist, so unless it is said here that amount
     * stays behind in the old period and nobody knows. The same warning that {@code seedQuotaRecords} emits on the immediate
     * carry path; this case is rare and has no automatic handling yet.
     */
    private void reportStrandedTypes(List<SchoolSubscriptionQuotaRecord> sourcePools,
            List<SchoolSubscriptionQuotaRecord> targets, UUID sourceSubscriptionId) {
        Set<QuotaType> carriedTypes = targets.stream()
            .map(record -> record.getQuotaType())
            .collect(Collectors.toSet());

        for (SchoolSubscriptionQuotaRecord pool : sourcePools) {
            BigDecimal unspentVnd = pool.unspentFundedVnd();
            if (unspentVnd.signum() > 0 && !carriedTypes.contains(pool.getQuotaType())) {
                log.warn("Period {} has {} VND of unspent self-funded money in quota {} but the next period does not have this"
                    + " quota type -- that amount stays behind and needs manual handling.",
                    sourceSubscriptionId, unspentVnd, pool.getQuotaType());
            }
        }
    }
}
