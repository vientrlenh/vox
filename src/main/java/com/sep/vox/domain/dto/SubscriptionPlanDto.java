package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SubscriptionPlanDto(
    UUID id,
    String name,
    String tagline,
    BigDecimal pricePerYear,
    Integer validityDays,
    Integer maxTimePerAttemptMin,
    Integer maxStudentCount,
    boolean popular,
    String status,
    Integer version,
    String createdAt,
    UUID createdBy,
    UUID replacedByPlanId,
    BigDecimal serviceFeeRatio,
    List<PlanQuotaDto> quotas
) {
}
