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
    SubscriptionPlanPeriod periodType, 
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    SubscriptionPlanStatus status,
    Long version,
    Instant createdAt, 
    Instant updatedAt, 
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
            plan.getPeriodType(), 
            plan.getPeriodCount(), 
            plan.getMaxTimePerAttemptMin(), 
            plan.getStatus(), 
            plan.getVersion(), 
            plan.getCreatedAt(), 
            plan.getUpdatedAt(), 
            plan.createdByUserId().orElse(null),
            plan.updatedByUserId().orElse(null),
            plan.getReplacedByPlanId()
        );
    }
}
