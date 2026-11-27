package com.sep.vox.domain.model.metering;

import com.sep.vox.domain.model.user.SchoolRoleCodes;

/**
 * Two school-level quota WALLETS, both in VND. Each value here is a SEPARATE pocket of money: spending one
 * does not touch the other, and every activity is deducted from EXACTLY ONE pocket.
 *
 * <p>Separating ASSESSMENT/PRACTICE is a BUSINESS decision and not an accounting one: if they shared one wallet,
 * one week of unusually heavy speaking practice by students would eat the whole budget meant for the end-of-term assessment. This
 * boundary coincides exactly with {@link QuotaPricingSource} -- the two AI pipelines are very different (evalGraph
 * grading an assessment is heavier than the realtimeCorrectionGraph of speaking practice) so two separate rates are also calibrated.
 *
 * <p>There is NO CLASS_TEST. It used to sit alongside here but is in essence a SPENDING CEILING INSIDE the ASSESSMENT
 * wallet, not a third wallet: a class test was charged the same
 * {@code totalCostUsd} twice -- once under GRADING, once under CLASS_TEST -- making
 * {@code SUM(used_amount_vnd)} always higher than the real money, and the constraint
 * {@code chk_school_balance_entries_no_class_test_charge} had to be built to stop that duplicate deduction from touching money.
 * The ceiling per TEACHER is still intact and is still a real feature -- it lives in
 * {@link com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation} with
 * quotaType = ASSESSMENT, and is only checked when the assessment has {@code kind = CLASS_TEST} (see
 * ClassTestTokenQuotaGuardService). That ceiling is a LIMIT, never a balance: it does not
 * hold money, does not record debt, and does not appear in school_balance_entries.
 *
 * <p>There is NO SPEAKING. The Azure voice live cost is a cost COMPONENT, not an activity --
 * it is already recorded in {@code ai_usage_record.usage_type = 'DURATION'}, per turn and
 * with the provider. Both assessments and speaking practice incur it, so building it into a third wallet would repeat exactly the
 * mistake of CLASS_TEST in another direction: an assessment would have to be deducted from two wallets, and worse than CLASS_TEST in that
 * the SPEAKING wallet would straddle BOTH activities so speaking practice could drain the assessment's share -- reopening exactly the
 * door that separating ASSESSMENT/PRACTICE was created to close. To know "how much did speaking cost this month",
 * GROUP BY ai_usage_record.usage_type, with no need for another wallet.
 */
public enum QuotaType {
    /** Assessment grading -- every AssessmentKind (both CENTRALIZED and CLASS_TEST) is deducted from here. */
    ASSESSMENT,
    /** A student practising speaking on their own. */
    PRACTICE;

    /**
     * The role that receives the personal spending ceiling of this wallet: ASSESSMENT is distributed to TEACHERS, PRACTICE to STUDENTS.
     *
     * <p>Placed here because three places need exactly one answer -- the two quota allocation use cases
     * (AllocateAssessmentQuotaToTeachers / AllocatePracticeQuotaToStudents) and the "still eligible" filter
     * when carrying the ceilings to a new period (OrderSettlementService.carryForwardUserAllocations). This mapping decides
     * whether a person keeps their ceiling across a renewal, so if the two sides diverge a real person loses their ceiling.
     */
    public String allocationRoleCode() {
        return switch (this) {
            case ASSESSMENT -> SchoolRoleCodes.TEACHER;
            case PRACTICE -> SchoolRoleCodes.STUDENT;
        };
    }
}
