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
    BigDecimal triggerAmountUsd,
    BigDecimal totalAllocatedUsd,
    BigDecimal usedQuantityUsd,
    BigDecimal overageUsd,
    String occurredAt
) {
}
