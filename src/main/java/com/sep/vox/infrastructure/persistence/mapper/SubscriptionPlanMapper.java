package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanJpaEntity;

public final class SubscriptionPlanMapper {

    private SubscriptionPlanMapper() {}

    public static SubscriptionPlan toDomain(SubscriptionPlanJpaEntity jpa) {
        return new SubscriptionPlan(
            jpa.getId(),
            jpa.getName(),
            jpa.getTagline(),
            jpa.getPriceVnd(),
            fromString(jpa.getPeriodType()), 
            jpa.getPeriodCount(),
            jpa.getMaxTimePerAttemptMin(),
            SubscriptionPlanStatus.valueOf(jpa.getStatus()),
            jpa.getVersion(),
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy(), 
            jpa.getReplacedByPlanId(),
            jpa.getServiceFeeRatio()
        );
    }

    public static SubscriptionPlanJpaEntity toJpa(SubscriptionPlan domain) {
        return new SubscriptionPlanJpaEntity(
            domain.getId(),
            domain.getName(),
            domain.getTagline(),
            domain.getPriceVnd(),
            valueOf(domain.getPeriodType()),
            domain.getPeriodCount(),
            domain.getMaxTimePerAttemptMin(),
            domain.getStatus().name(),
            domain.getVersion(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(), 
            domain.getCreatedBy(), 
            domain.getUpdatedBy(), 
            domain.getReplacedByPlanId(),
            domain.getServiceFeeRatio()
        );
    }

    private static SubscriptionPlanPeriod fromString(String periodType) {
        return periodType == null ? null : SubscriptionPlanPeriod.valueOf(periodType);
    }

    private static String valueOf(SubscriptionPlanPeriod period) {
        return period == null ? null : period.name();
    }
}
