package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.SubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.model.subscription.SubscriptionQuotaUserAllocation;

public final class SubscriptionQuotaUserAllocationDtoMapper {

    private SubscriptionQuotaUserAllocationDtoMapper() {
    }

    public static SubscriptionQuotaUserAllocationDto toDto(SubscriptionQuotaUserAllocation domain, String fullName) {
        return new SubscriptionQuotaUserAllocationDto(
            domain.getUserId(),
            fullName,
            domain.getQuotaType().name(),
            domain.getAllocatedQuantity(),
            domain.getUsedQuantity()
        );
    }
}
