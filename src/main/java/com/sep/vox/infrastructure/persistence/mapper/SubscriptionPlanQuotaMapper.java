package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanQuotaJpaEntity;

public final class SubscriptionPlanQuotaMapper {

    private SubscriptionPlanQuotaMapper() {}

    public static SubscriptionPlanQuota toDomain(SubscriptionPlanQuotaJpaEntity jpa) {
        return new SubscriptionPlanQuota(
            jpa.getId(),
            jpa.getPlanId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getIncludedQuantity(),
            jpa.getTokenUnitPrice()
        );
    }

    public static SubscriptionPlanQuotaJpaEntity toJpa(SubscriptionPlanQuota domain) {
        return new SubscriptionPlanQuotaJpaEntity(
            domain.getId(),
            domain.getPlanId(),
            domain.getQuotaType().name(),
            domain.getIncludedQuantity(),
            domain.getTokenUnitPrice()
        );
    }
}
