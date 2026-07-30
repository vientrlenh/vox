package com.sep.vox.domain.dto;

import java.util.UUID;

public record SubscriptionQuotaUserAllocationDto(
    UUID userId,
    String fullName,
    String quotaType,
    Integer allocatedQuantity,
    Integer usedQuantity
) {
}
