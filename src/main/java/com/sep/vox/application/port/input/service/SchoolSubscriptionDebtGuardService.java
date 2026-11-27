package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.repository.SchoolBalanceRepository;

/**
 * A school is "locked" when its self-funded balance is negative, i.e. the real AI cost has exceeded both the plan quota and
 * the money the school has topped up -- see {@link SchoolBalance#isInDebt()}.
 *
 * <p>Locked per SCHOOL, not per subscription, which is why the parameter here is {@code schoolId}:
 * the balance belongs to the school and lives across every renewal/plan change (V2 section 2), so accepting
 * {@code subscriptionId} and looking back up would wrongly imply that the debt belongs to some subscription period.
 *
 * <p>NO LONGER split by QuotaType. Before, debt was {@code used_amount_vnd > total_allocated_amount_vnd}
 * of EACH quota record so you could ask "is the assessment quota in debt"; now debt lives in the balance -- a single pocket of the
 * school -- so that question no longer has data to answer it. The quota record only expresses "how much the plan grants /
 * how much of it has been spent", and used never exceeds total any more (see
 * ConsumeQuotaService).
 *
 * <p>There is no separate flag/table storing the lock state -- it is derived directly from the balance on every call so it is always
 * exact and immediate, and it "unlocks" itself as soon as the school tops up and brings the balance back to non-negative, with no
 * reconcile step.
 */
@Service
@RequiredArgsConstructor
public class SchoolSubscriptionDebtGuardService {

    private final SchoolBalanceRepository schoolBalanceRepository;

    /**
     * No balance row = the school has never topped up and never overspent the quota = NOT in debt.
     * An empty balance and a balance of 0 mean the same thing -- see {@link SchoolBalance#emptyFor}.
     */
    public boolean isSchoolLocked(UUID schoolId) {
        return schoolBalanceRepository.findBySchoolId(schoolId)
            .map(b -> b.isInDebt())
            .orElse(false);
    }

    public void requireSchoolNotLocked(UUID schoolId) {
        if (isSchoolLocked(schoolId)) {
            throw new BusinessRuleViolationException(
                "The school is locked because the actual AI cost exceeded the quota; please pay or renew/upgrade the plan to continue using the service"
            );
        }
    }
}
