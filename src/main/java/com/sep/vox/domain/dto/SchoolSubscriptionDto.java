package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SchoolSubscriptionDto(
    UUID id,
    UUID schoolId,
    UUID planId,
    String startDate,
    String endDate,
    String status,
    BigDecimal pricePaidSnapshot,
    String cancelledAt,
    String createdAt
) {
}
