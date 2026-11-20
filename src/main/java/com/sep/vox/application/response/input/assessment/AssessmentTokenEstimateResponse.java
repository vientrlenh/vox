package com.sep.vox.application.response.input.assessment;

import java.math.BigDecimal;

/**
 * A warning (NOT a block) with the worst-case estimate of AI cost + the remaining ability to pay -- see
 * ClassTestTokenQuotaGuardService.estimateTokenQuota.
 *
 * <p>Everything is in VND at COST PRICE, without the service fee: the fee is charged once when the school places the order
 * (CreateTopUpOrderUseCase), while both the quota wallet and the self-topped-up wallet are recorded at cost price -- see ServiceFeePort.
 * Adding the fee here would make the estimate about 20% higher than the amount actually deducted.
 *
 * @param remainingAssessmentVnd  the money the school CAN STILL SPEND from the ASSESSMENT wallet = remaining plan quota +
 *                                the self-topped-up wallet balance (non-negative part). null if the school has no
 *                                active subscription or has not configured this wallet
 * @param remainingMyClassTestVnd the REMAINING PERSONAL quota of the teacher who authors the test -- NOT a second
 *                                school-level wallet (the CLASS_TEST wallet was removed, see QuotaType), and
 *                                it does NOT add the school wallet balance because this is an internal spending ceiling and not
 *                                a pocket of money. null if the assessment is not a CLASS_TEST or the teacher
 *                                has no separate quota, i.e. only the school's money applies
 * @param schoolLocked            the school is locked because of debt (negative self-topped-up balance) -- see
 *                                SchoolSubscriptionDebtGuardService.isSchoolLocked. A DIFFERENT cause
 *                                from wouldExceedAssessment (quota exhausted): it can be true even though
 *                                wouldExceedAssessment=false if the plan quota still has room, because
 *                                remainingAssessmentVnd deliberately clamps a negative balance to 0 (see
 *                                ClassTestTokenQuotaGuardService.spendableSchoolFundsVnd)
 * @param sharedPoolUsageRatio    the % of the REMAINING PLAN QUOTA of the ASSESSMENT wallet that the estimated cost of this
 *                                assessment takes (without adding the self-topped-up balance) -- only has a value when
 *                                assessment.kind == CENTRALIZED and the remaining plan quota is > 0. CENTRALIZED
 *                                has no personal quota so it easily "eats" into the shared wallet part that other teachers
 *                                (who have their own personal quota for CLASS_TEST) know nothing about
 *                                beforehand -- see teachersWithUnusedPersonalAllocationCount. null for
 *                                CLASS_TEST or when it cannot apply (no subscription yet, or the plan
 *                                wallet is already exhausted -- then wouldExceedAssessment/schoolLocked already say it)
 * @param teachersWithUnusedPersonalAllocationCount number of teachers (of this subscription) who still have
 *                                personal quota NOT fully used for CLASS_TEST (quotaType=ASSESSMENT). Only
 *                                has a value under the same condition as sharedPoolUsageRatio
 */
public record AssessmentTokenEstimateResponse(
    BigDecimal estimatedCostVnd,
    BigDecimal remainingAssessmentVnd,
    BigDecimal remainingMyClassTestVnd,
    boolean wouldExceedAssessment,
    boolean wouldExceedMyClassTest,
    boolean schoolLocked,
    BigDecimal sharedPoolUsageRatio,
    Integer teachersWithUnusedPersonalAllocationCount
) {
}
