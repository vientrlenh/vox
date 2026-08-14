package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionQuotaUserAllocationDto(
    UUID userId,
    String fullName,
    String quotaType,
    BigDecimal allocatedQuantity,
    BigDecimal usedQuantity
) {
}
