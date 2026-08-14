package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanJpaEntity;

public final class SubscriptionPlanMapper {

    private SubscriptionPlanMapper() {}

    public static SubscriptionPlan toDomain(SubscriptionPlanJpaEntity jpa) {
        return new SubscriptionPlan(
            jpa.getId(),
            jpa.getName(),
            jpa.getTagline(),
            jpa.getPricePerYear(),
            jpa.getValidityDays(),
            jpa.getMaxTimePerAttemptMin(),
            jpa.getMaxStudentCount(),
            PlanStatus.valueOf(jpa.getStatus()),
            jpa.getVersion(),
            jpa.getCreatedAt(),
            jpa.getCreatedBy(),
            jpa.getReplacedByPlanId(),
            jpa.getServiceFeeRatio()
        );
    }

    public static SubscriptionPlanJpaEntity toJpa(SubscriptionPlan domain) {
        return new SubscriptionPlanJpaEntity(
            domain.getId(),
            domain.getName(),
            domain.getTagline(),
            domain.getPricePerYear(),
            domain.getValidityDays(),
            domain.getMaxTimePerAttemptMin(),
            domain.getMaxStudentCount(),
            domain.getStatus().name(),
            domain.getVersion(),
            domain.getCreatedAt(),
            domain.getCreatedBy(),
            domain.getReplacedByPlanId(),
            domain.getServiceFeeRatio()
        );
    }
}
