package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;

public record SubscriptionPlanQuotaDto(
    UUID id,
    UUID subscriptionPlanId,
    String quotaType,
    BigDecimal includedAmountVnd
) {

    public static SubscriptionPlanQuotaDto toDto(SubscriptionPlanQuota quota) {
        return new SubscriptionPlanQuotaDto(
            quota.getId(),
            quota.getSubscriptionPlanId(),
            valueOf(quota.getQuotaType()),
            quota.getIncludedAmountVnd()
        );
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
