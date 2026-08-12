package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionQuotaDto(
    UUID id,
    UUID subscriptionId,
    String quotaType,
    BigDecimal totalAllocated,
    BigDecimal usedQuantity
) {
}
