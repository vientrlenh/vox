package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.infrastructure.persistence.entity.subscription.SubscriptionPlanJpaEntity;

public final class SubscriptionPlanMapper {

    private SubscriptionPlanMapper() {}

    public static SubscriptionPlan toDomain(SubscriptionPlanJpaEntity jpa) {
        return SubscriptionPlan.builder()
            .id(jpa.getId())
            .name(jpa.getName())
            .tagline(jpa.getTagline())
            .priceVnd(jpa.getPriceVnd())
            .periodType(periodFromString(jpa.getPeriodType()))
            .periodCount(jpa.getPeriodCount())
            .maxTimePerAttemptMin(jpa.getMaxTimePerAttemptMin())
            .status(statusFromString(jpa.getStatus()))
            .version(jpa.getVersion())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .replacedByPlanId(jpa.getReplacedByPlanId())
            .build();
    }

    public static SubscriptionPlanJpaEntity toJpa(SubscriptionPlan domain) {
        return SubscriptionPlanJpaEntity.builder()
            .id(domain.getId())
            .name(domain.getName())
            .tagline(domain.getTagline())
            .priceVnd(domain.getPriceVnd())
            .periodType(valueOf(domain.getPeriodType()))
            .periodCount(domain.getPeriodCount())
            .maxTimePerAttemptMin(domain.getMaxTimePerAttemptMin())
            .status(valueOf(domain.getStatus()))
            .version(domain.getVersion())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .createdBy(domain.getCreatedBy())
            .updatedBy(domain.getUpdatedBy())
            .replacedByPlanId(domain.getReplacedByPlanId())
            .build();
    }

    private static SubscriptionPlanPeriod periodFromString(String periodType) {
        if (periodType == null)
            return null;
        try {
            return SubscriptionPlanPeriod.valueOf(periodType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid period type when converting to the domain model: " + periodType);
        }
    }

    private static SubscriptionPlanStatus statusFromString(String status) {
        if (status == null)
            return null;
        try {
            return SubscriptionPlanStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status when converting to the domain model: " + status);
        }
    }

    private static String valueOf(SubscriptionPlanPeriod period) {
        return period == null ? null : period.name();
    }

    private static String valueOf(SubscriptionPlanStatus status) {
        return status == null ? null : status.name();
    }
}
