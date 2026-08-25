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
            periodFromString(jpa.getPeriodType()), 
            jpa.getPeriodCount(),
            jpa.getMaxTimePerAttemptMin(),
            statusFromString(jpa.getStatus()),
            jpa.getVersion(),
            jpa.getCreatedAt(), 
            jpa.getUpdatedAt(), 
            jpa.getCreatedBy(), 
            jpa.getUpdatedBy(), 
            jpa.getReplacedByPlanId()
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
            valueOf(domain.getStatus()),
            domain.getVersion(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(), 
            domain.getCreatedBy(), 
            domain.getUpdatedBy(), 
            domain.getReplacedByPlanId()
        );
    }

    private static SubscriptionPlanPeriod periodFromString(String periodType) {
        if (periodType == null)
            return null;
        try {
            return SubscriptionPlanPeriod.valueOf(periodType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại chu kỳ khi chuyển đổi sang domain model không hợp lệ: " + periodType);
        }
    }

    private static SubscriptionPlanStatus statusFromString(String status) {
        if (status == null)
            return null;
        try {
            return SubscriptionPlanStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái khi chuyển đổi sang domain model không hợp lệ: " + status);
        }
    }

    private static String valueOf(SubscriptionPlanPeriod period) {
        return period == null ? null : period.name();
    }

    private static String valueOf(SubscriptionPlanStatus status) {
        return status == null ? null : status.name();
    }
}
