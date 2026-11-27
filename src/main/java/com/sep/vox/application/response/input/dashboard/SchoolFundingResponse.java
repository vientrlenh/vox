package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolBalance;

/**
 * Whether the school can still open assessment sessions, and how much it can still grade before running out.
 *
 * <p>Two DIFFERENT pockets of money, deliberately not merged into one number:
 *
 * <ul>
 *   <li>The plan quota, split by {@code QuotaType} -- only the {@code ASSESSMENT} wallet pays for assessment
 *       grading. A roomy {@code PRACTICE} wallet does not help next week's assessment.
 *   <li>The self-topped-up wallet -- ONE shared number, only deducted when the quota of the corresponding kind is exhausted.
 * </ul>
 *
 * <p>MONEY HERE IS String, unlike {@code tokenAllocated}/{@code tokenUsed} right next to it. The reason is written at the
 * top of school-balance.graphqls: the balance is {@code numeric(18,6)} so a double rounds wrongly exactly where the
 * number has to match absolutely. Mixing Float and String in the SAME money block is harder to understand than being inconsistent with the
 * neighboring block, so every money field here uses one type.
 */
public record SchoolFundingResponse(
    /** The remaining ASSESSMENT quota. Never negative -- the excess has been turned into debt on the wallet. */
    String assessmentQuotaRemainingVnd,
    String assessmentQuotaTotalVnd,
    /** The top-up wallet balance. Negative IS the debt. */
    String balanceVnd,
    boolean locked,
    /** The remaining assessment quota plus the wallet balance if positive -- the money that can actually still pay for grading. */
    String spendableVnd,
    /** The part promised to teachers that they have not spent yet. Never negative. */
    String committedToUsersVnd,
    /** {@code spendable - committedToUsers}. NEGATIVE = the school has promised more than the money it has left. */
    String uncommittedVnd
) {

    /**
     * {@code locked} asks {@link SchoolBalance#isInDebt()} directly instead of retyping the comparison with 0: that is
     * exactly the function {@code SchoolSubscriptionDebtGuardService} calls to block opening assessment sessions, and a rule
     * should have only one place that defines it.
     *
     * <p>A negative wallet is NOT added into {@code spendable}: debt is not a negative quota but an amount that must be
     * paid, and subtracting it from the remaining quota would draw "3 million left" for a school that is hard locked.
     *
     * <p><b>{@code spendable} is NOT reduced by {@code committedToUsers}</b>, even though the two numbers sit next to each
     * other. {@code spendable} is the LOCK predicate -- it must match, to the dong,
     * {@code ClassTestTokenQuotaGuardService.spendableSchoolFundsVnd}, the door that actually refuses to schedule an
     * assessment, and it is also the denominator of the "running low" warning band. Subtracting the promised part from it would make the dashboard
     * report no money while the system still allows spending -- a far worse discrepancy than the current number.
     * {@code uncommitted} answers a DIFFERENT question: how much the school is free to use on a centralized assessment
     * without eating into what was promised to teachers.
     *
     * @param committedToUsersVnd the total UNSPENT part of the personal spending caps, clamped at 0 per row --
     *                            see {@code SchoolSubscriptionQuotaUserAllocationRepository
     *                            .sumUnusedAllocation}. Subtracting the total {@code allocated} instead of the unspent
     *                            part would subtract twice the very money the teachers already spent.
     * @param balance             null when the school has never touched the wallet -- an empty wallet and a 0-dong wallet mean
     *                            the same thing.
     */
    public static SchoolFundingResponse of(BigDecimal assessmentQuotaTotalVnd, BigDecimal assessmentQuotaUsedVnd,
            BigDecimal committedToUsersVnd, SchoolBalance balance) {
        BigDecimal remaining = assessmentQuotaTotalVnd.subtract(assessmentQuotaUsedVnd).max(BigDecimal.ZERO);
        BigDecimal balanceVnd = balance == null ? BigDecimal.ZERO : balance.getBalanceVnd();
        boolean locked = balance != null && balance.isInDebt();
        BigDecimal spendable = remaining.add(balanceVnd.max(BigDecimal.ZERO));
        BigDecimal committed = committedToUsersVnd == null ? BigDecimal.ZERO : committedToUsersVnd;

        return new SchoolFundingResponse(
            DecimalText.of(remaining),
            DecimalText.of(assessmentQuotaTotalVnd),
            DecimalText.of(balanceVnd),
            locked,
            DecimalText.of(spendable),
            DecimalText.of(committed),
            // NOT clamped to 0: a negative number here is the most alarming thing this screen can say -- the school has
            // promised teachers more than the money it has left, so the last person to spend will be refused by the
            // school wallet even though their personal cap still has room. Clamping to 0 hides exactly that warning.
            DecimalText.of(spendable.subtract(committed))
        );
    }
}
