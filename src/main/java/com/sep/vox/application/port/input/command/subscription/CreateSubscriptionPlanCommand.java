package com.sep.vox.application.port.input.command.subscription;

import java.math.BigDecimal;
import java.util.List;

import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;

/**
 * No serviceFeeRatio any more: the service fee became a SYSTEM-WIDE config
 * (vox.quota.selling-price.service-fee-ratio) and is added as a separate line on the ORDER, and is
 * neither stored per plan nor folded into the exchange rate -- see {@code QuotaPricingPort#serviceFeeRatio()}.
 *
 * <p>Plan cycle = periodType x periodCount (e.g. MONTH x 12) replacing the old validityDays: for a plan counted in
 * months/years the end date must land on the matching calendar date, and adding a raw number of days drifts further off.
 */
public record CreateSubscriptionPlanCommand(
    String name,
    String tagline,
    BigDecimal priceVnd,
    SubscriptionPlanPeriod periodType,
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    List<CreateSubscriptionPlanQuotaCommand> quotas
) {
}
