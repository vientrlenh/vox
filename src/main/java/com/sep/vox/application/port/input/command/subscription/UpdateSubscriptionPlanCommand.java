package com.sep.vox.application.port.input.command.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Every field (except id) ALLOWS null meaning "do not touch" -- this is a partial update, not
 * an overwrite of the whole plan.
 *
 * <p>No periodType: the period_type column is {@code updatable = false} so the cycle chosen at creation
 * cannot be changed. A plan counted in MONTHS edited to YEARS would give every end date computed earlier
 * a different meaning -- to change it, create a new plan and point replacedByPlanId at it.
 *
 * <p>No serviceFeeRatio: the service fee became a SYSTEM-WIDE config
 * (vox.quota.selling-price.service-fee-ratio) and is no longer stored per plan -- see
 * {@code QuotaPricingPort#serviceFeeRatio()}.
 *
 * <p>quotas null = keep the current set of quotas. If passed, the list is treated as the WHOLE new
 * set of quotas (replacing everything), not as an addition.
 */
public record UpdateSubscriptionPlanCommand(
    UUID subscriptionPlanId,
    String name,
    String tagline,
    BigDecimal priceVnd,
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    List<UpdateSubscriptionPlanQuotaCommand> quotas
) {
}
