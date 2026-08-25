package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;

public record SubscriptionPlanDto(
    UUID id,
    String name,
    String tagline,
    BigDecimal priceVnd, 
    String periodType, 
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    String status,
    Long version,
    String createdAt, 
    String updatedAt, 
    UUID createdBy, 
    UUID updatedBy, 
    UUID replacedByPlanId
) {

    public static SubscriptionPlanDto toDto(SubscriptionPlan plan) {
        return new SubscriptionPlanDto(
            plan.getId(), 
            plan.getName(), 
            plan.getTagline(), 
            plan.getPriceVnd(), 
            valueOf(plan.getPeriodType()), 
            plan.getPeriodCount(), 
            plan.getMaxTimePerAttemptMin(), 
            valueOf(plan.getStatus()), 
            plan.getVersion(), 
            fromInstant(plan.getCreatedAt()), 
            fromInstant(plan.getUpdatedAt()), 
            plan.getCreatedBy(), 
            plan.getUpdatedBy(), 
            plan.getReplacedByPlanId()
        );
    }

    private static String valueOf(SubscriptionPlanPeriod period) {
        return period == null ? null : period.name();
    }

    private static String valueOf(SubscriptionPlanStatus status) {
        return status == null ? null : status.name();
    }

    private static String fromInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
