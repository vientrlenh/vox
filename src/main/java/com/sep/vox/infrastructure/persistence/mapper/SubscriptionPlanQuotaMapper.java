package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.infrastructure.persistence.entity.subscription.SubscriptionPlanQuotaJpaEntity;

public final class SubscriptionPlanQuotaMapper {

    private SubscriptionPlanQuotaMapper() {}

    public static SubscriptionPlanQuota toDomain(SubscriptionPlanQuotaJpaEntity jpa) {
        return SubscriptionPlanQuota.builder()
            .id(jpa.getId())
            .subscriptionPlanId(jpa.getSubscriptionPlanId())
            .quotaType(fromString(jpa.getQuotaType()))
            .includedAmountVnd(jpa.getIncludedAmountVnd())
            .build();
    }

    public static SubscriptionPlanQuotaJpaEntity toJpa(SubscriptionPlanQuota domain) {
        return SubscriptionPlanQuotaJpaEntity.builder()
            .id(domain.getId())
            .subscriptionPlanId(domain.getSubscriptionPlanId())
            .quotaType(valueOf(domain.getQuotaType()))
            .includedAmountVnd(domain.getIncludedAmountVnd())
            .build();
    }

    private static QuotaType fromString(String type) {
        if (type == null) 
            return null;
        try {
            return QuotaType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quota type when converting to the domain model: " + type);
        }
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
