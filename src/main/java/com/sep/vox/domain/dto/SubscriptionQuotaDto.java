package com.sep.vox.domain.dto;

import java.util.UUID;

public record SubscriptionQuotaDto(
    UUID id,
    UUID subscriptionId,
    String quotaType,
    Integer totalAllocated,
    Integer usedQuantity
) {
}
