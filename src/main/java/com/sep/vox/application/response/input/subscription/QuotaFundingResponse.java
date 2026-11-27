package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * The result of one top-up from the top-up wallet into the quota wallet.
 *
 * <p>Returns BOTH sides of the transfer because the screen must update both numbers at once, and because this is
 * the only way for the person who just clicked to see immediately that the money went to the right place -- an operation that cannot
 * be undone should not make people open another screen to check.
 *
 * @param pool             the quota wallet AFTER the top-up -- the {@code fundedFromBalanceVnd} in it is the
 *                         running total of the period, not only this top-up
 * @param fundedAmountVnd  the amount just transferred, always positive
 * @param balanceAfterVnd  the remaining top-up wallet balance, matching exactly the entry just recorded
 */
public record QuotaFundingResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    BigDecimal fundedAmountVnd,
    BigDecimal balanceAfterVnd
) {
}
