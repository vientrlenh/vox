package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialEventDto(
    UUID id,
    UUID schoolId,
    UUID subscriptionId,
    String eventType,
    BigDecimal amountSigned,
    String currency,
    UUID actorId,
    String occurredAt
) {
}
