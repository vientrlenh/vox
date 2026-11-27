package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * ONE PAGE of the personal quota allocation screen, with two totals for the whole set.
 *
 * <p>Replaces {@code QuotaUserAllocationSummaryResponse}, which returned ALL eligible people in
 * one go: a school with several thousand students means several thousand rows each time the page opens, and the UI
 * cannot build that many rows either.
 *
 * <p><b>Why {@code distributedAmountVnd} has to be here.</b> Previously the UI summed the allocation column of
 * every row itself to know how much the school had distributed and how much remained. When only one page
 * is in hand that sum gives a WRONG number -- so the total must be computed on the server over the whole set,
 * in the same mold as {@code SchoolBalanceSummaryResponse}.
 *
 * @param pool                   the school-level quota wallet -- ASSESSMENT when distributing to teachers, PRACTICE when
 *                               distributing to students
 * @param distributedAmountVnd   the total distributed to people who are STILL eligible, over the whole set and
 *                               not just this page. People who have left/graduated are NOT added
 *                               -- see {@code orphanedAmountVnd}
 * @param orphanedAmountVnd      the part still held under the names of people who are NO LONGER eligible (deactivated,
 *                               left the school, changed role). It does not count toward the ceiling and does not appear on
 *                               any page of the table, so it has to be said in words here: otherwise the total on screen and the sum of the rows
 *                               differ and nobody can explain why. Usually 0
 * @param distributableRatio     the school's distribution ceiling for this quota type, 0..1
 * @param distributableAmountVnd the part of the wallet allowed to be distributed = pool x distributableRatio. Returned ready-made instead
 *                               of letting the client multiply: this is the number the backend uses to reject, so
 *                               a second multiplication on the client is a chance for the two sides to diverge
 *                               by a few millionths of a dong and then report an error where the user cannot understand why
 * @param spendableFundsVnd      the money the school can REALLY still pay for this quota type = remaining plan
 *                               quota + self-topped-up wallet balance (clamped to 0). COMPLETELY DIFFERENT from
 *                               distributableAmountVnd, which is computed on total_allocated and does not
 *                               decrease when the wallet is spent: a school that has spent its wallet can still distribute
 *                               within the ceiling, and that ceiling says nothing about the ability to pay.
 *                               The UI uses it to WARN, not to block -- a spending ceiling is a
 *                               ceiling, and the real block already exists at the gates when opening an assessment / building a practice
 *                               paper (ClassTestTokenQuotaGuardService, findPracticeSpendableFundsVnd)
 * @param walletBalanceVnd       the part of the school's SELF-TOPPED-UP wallet (school_balances) that CAN be eaten into beyond
 *                               distributableAmountVnd when loosening the personal ceiling of ONE person -- not
 *                               a reserved pocket of money, and SHARED by both ASSESSMENT and PRACTICE
 *                               (see the BusinessRuleViolationException thrown when a wallet draw is not confirmed). Clamped to 0, negative is debt.
 * @param content                the eligible people on the page being viewed, including those who have been allocated nothing
 */
public record QuotaUserAllocationPageResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    BigDecimal distributedAmountVnd,
    BigDecimal orphanedAmountVnd,
    BigDecimal distributableRatio,
    BigDecimal distributableAmountVnd,
    BigDecimal spendableFundsVnd,
    BigDecimal walletBalanceVnd,
    List<Row> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    /**
     * One person in the quota allocation list.
     *
     * <p>Does NOT carry the user's name: the name is joined in at the GraphQL layer through the data loader {@code userById},
     * so a page of 20 rows costs EXACTLY ONE extra users query instead of 20. This is also the fix for the bug where the "Full name"
     * column always showed a dash: {@code SchoolSubscriptionQuotaUserAllocationDto} never had
     * a name field, while the client declared such a field and never received a value.
     *
     * <p><b>{@code allocatedAmountVnd} null = NOT YET ALLOCATED, very different from 0.</b> The two states produce
     * OPPOSITE behavior and used to be merged into the same 0 on screen:
     *
     * <ul>
     *   <li>no allocation row (null) = NOT blocked per person. For a teacher it means spending
     *       freely within the school wallet ({@code ClassTestTokenQuotaGuardService.remainingUserAllocation}
     *       returns null and the guard skips it); for a student it is the opposite, the LEAST(...) in
     *       {@code findPracticeSpendableFundsVnd} pulls it to 0 so they cannot practice a single turn.</li>
     *   <li>a row that equals 0 = fully blocked, for both kinds.</li>
     * </ul>
     *
     * <p>So a school with half its teachers showing "0 ₫" may in fact have half its teachers with NO spending cap
     * at all -- and on the old table those two cases looked identical. The client must show two different
     * labels ("Not allocated" vs "0 ₫"), so the data type here must be able to tell them apart.
     *
     * <p>{@code usedAmountVnd} always has a number: no row means nothing was spent through a personal cap,
     * and 0 there is not ambiguous.
     */
    public record Row(
        UUID userId,
        BigDecimal allocatedAmountVnd,
        BigDecimal usedAmountVnd
    ) {
    }
}
