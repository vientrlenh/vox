package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionRequestDto(
    UUID id,
    UUID schoolId,
    String requestType,
    UUID currentPlanId,
    UUID requestedPlanId,
    BigDecimal amount,
    String status,
    String submittedAt,
    UUID reviewedBy,
    String reviewedAt
) {
}
