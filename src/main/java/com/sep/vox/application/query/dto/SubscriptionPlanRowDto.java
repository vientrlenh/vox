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
    boolean popular,
    String status,
    Integer version,
    String createdAt,
    UUID createdBy
) { }
