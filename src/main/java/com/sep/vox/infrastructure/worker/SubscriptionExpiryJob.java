package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.CarryQuotaFundingAtPeriodStartService;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

// Before, NOTHING automatically moved an ACTIVE subscription past endDate to EXPIRED -- the status only changed
// when a NEW subscription overrode it (renewal / admin approving another request). This job fills exactly that gap, and
// is also the "cut off access" mechanism for the cancel-midway case in the Claude style: cancelling only sets cancelledAt (see
// CancelSubscriptionUseCase), the subscription stays ACTIVE and fully usable until endDate, then
// this job moves it to EXPIRED when that day arrives -- it does not need to know whether the subscription was cancelled,
// expired is expired.
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryJob {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final CarryQuotaFundingAtPeriodStartService carryQuotaFundingAtPeriodStartService;

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void expireOverdueSubscriptions() {
        Instant today = Instant.now();
        int expiredCount = schoolSubscriptionRepository.expireOverdue(today);
        if (expiredCount > 0) {
            log.info("Moved {} overdue subscriptions to EXPIRED", expiredCount);
        }

        // The boundary between two periods is the ONLY place where the self-funded amount to carry over can be fixed, and this job is already
        // what runs at that boundary -- see CarryQuotaFundingAtPeriodStartService and V13. The order relative to the
        // sweep above does not matter: the carry-over attaches to the start_date of the NEW period, not to the status
        // of the old one, precisely because the new period can be spent right when its date arrives while the other sweep may lag by an hour.
        int carriedCount = carryQuotaFundingAtPeriodStartService.carryDueFunding(today);
        if (carriedCount > 0) {
            log.info("Carried unspent self-funded money over to {} quota wallets of the period that just started", carriedCount);
        }
    }
}
