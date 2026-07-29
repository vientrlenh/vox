package com.sep.vox.domain.dto;

import java.util.List;

public record QuotaUserAllocationSummaryDto(
    SubscriptionQuotaDto pool,
    List<SubscriptionQuotaUserAllocationDto> allocations
) {
}
