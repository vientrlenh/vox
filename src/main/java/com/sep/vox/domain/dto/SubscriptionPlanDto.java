package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionPlanDto(
    UUID id,
    String name,
    String tagline,
    BigDecimal priceVnd, 
    String periodType, 
    Integer periodCount,
    Integer maxTimePerAttemptMin,
    String status,
    Integer version,
    String createdAt, 
    String updatedAt, 
    UUID createdBy, 
    UUID updatedBy, 
    UUID replacedByPlanId,
    BigDecimal serviceFeeRatio
) {
}
