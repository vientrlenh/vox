package com.sep.vox.interfaces.graphql.input.subscription;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Matches the input SubscriptionPlanQuotaInput in subscription.graphqls. quotaType keeps the enum
 * type because the schema declares QuotaType! -- graphql-java rejects unknown values already at parse time, so by
 * the time it gets here the value is certainly valid.
 */
public record SubscriptionPlanQuotaInput(
    QuotaType quotaType,
    BigDecimal includedAmountVnd
) {
}
