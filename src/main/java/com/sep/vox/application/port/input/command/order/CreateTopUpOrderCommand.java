package com.sep.vox.application.port.input.command.order;

import java.math.BigDecimal;

/**
 * creditAmountVnd is the BALANCE THE SCHOOL WANTS TO RECEIVE, not including the service fee -- the real amount to pay is
 * {@code orders.total_amount_vnd} = creditAmountVnd + charged_fee_vnd. It takes this direction instead of
 * receiving the total and subtracting backwards, because a school always thinks "I want 1 million more to spend",
 * and because the other way round the fee becomes the remainder of a division, off by a few dong from rounding.
 *
 * <p>Carries no schoolId: taken from the token of the logged-in school admin -- see
 * {@link CreateSubscriptionOrderCommand}.
 *
 * <p>No subscriptionId and no list by QuotaType like the old BuyTokensCommand: the balance
 * now belongs to the SCHOOL (school_balances) and not to one subscription, and it is ONE single
 * number not split by quota type. Bringing subscriptionId in here would reconnect exactly what this refactor
 * just cut -- money topped up by the school must not die with the plan.
 */
public record CreateTopUpOrderCommand(
    BigDecimal creditAmountVnd
) {
}
