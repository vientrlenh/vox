package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SchoolDebtEventDto(
    UUID id,
    UUID schoolId,
    UUID subscriptionId,
    String eventType,
    String quotaType,
    UUID triggerExamSessionId,
    BigDecimal triggerAmountVnd,
    BigDecimal totalAllocatedVnd,
    BigDecimal usedAmountVnd,
    BigDecimal overageVnd,
    String occurredAt
) {
}
