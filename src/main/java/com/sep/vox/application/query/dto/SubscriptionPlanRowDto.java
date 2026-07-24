package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionPlanRowDto(
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
    UUID createdBy
) { }
