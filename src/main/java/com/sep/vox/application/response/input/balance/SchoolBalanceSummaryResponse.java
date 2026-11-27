package com.sep.vox.application.response.input.balance;

import java.math.BigDecimal;

import com.sep.vox.domain.common.DecimalText;

/**
 * The wallet ledger accumulated over a time range, grouped into four groups of entries.
 *
 * <p>These four groups cover ALL five entry types (TOP_UP and REFUND merged into one), and that is the condition for the
 * total box to do its job: explain every change of the balance within the range. Adding a new entry type
 * without adding a group here makes the statement show a wallet that dropped for no visible reason -- exactly the case
 * QUOTA_FUNDING caused once.
 *
 * <p>It is the RESPONSE of a use case and not a DTO in the domain, unlike {@code SchoolBalanceDto} and
 * {@code SchoolBalanceEntryDto}: those two map 1-1 from a real domain model, while this one has
 * no model behind it -- it is a use-case result assembled from five separate accumulations. Placed in
 * domain/dto it would force inventing a "SchoolBalanceSummary" that the business does not have.
 *
 * <p>It must be computed in the DB and cannot be built on the client: the client holds only one page of 20 rows, while the range to accumulate
 * may have thousands. It is also the source of the "deducted in the last 30 days" number on the Wallet Balance page.
 *
 * <p>DELIBERATELY has no entry count: that number is already in {@code totalElements} of the statement page, and
 * two places counting the same thing are two places that can disagree.
 */
public record SchoolBalanceSummaryResponse(
    String creditedVnd,
    String overageChargedVnd,
    String quotaFundedVnd,
    String adjustedVnd
) {

    /**
     * @param topUp        total TOP_UP  ({@code >= 0})
     * @param refund       total REFUND  ({@code >= 0}) -- merged with top-ups because to the reader of the statement
     *                     both are "money into the wallet"; separating them makes them add it up themselves
     * @param overage      total OVERAGE_CHARGE ({@code <= 0})
     * @param quotaFunding total QUOTA_FUNDING ({@code <= 0}) -- NOT merged into overage even though the sign is the same:
     *                     money moved to the quota wallet has not been spent, and merging would report a spend that did not happen
     * @param adjustment   total ADJUSTMENT (may have either sign)
     */
    public static SchoolBalanceSummaryResponse of(
            BigDecimal topUp, BigDecimal refund, BigDecimal overage, BigDecimal quotaFunding,
            BigDecimal adjustment) {
        return new SchoolBalanceSummaryResponse(
            DecimalText.orZero(nullSafe(topUp).add(nullSafe(refund))),
            DecimalText.orZero(overage),
            DecimalText.orZero(quotaFunding),
            DecimalText.orZero(adjustment)
        );
    }

    // The repository already COALESCEs to 0, but when adding two values a null that slips through would become a
    // NullPointerException in exactly the "school has no entries yet" case -- the most common one.
    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
