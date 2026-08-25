package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;

public record SchoolSubscriptionQuotaUserAllocationDto(
    UUID id, 
    UUID userId,
    UUID schoolSubscriptionId, 
    String fullName,
    String quotaType,
    BigDecimal allocatedAmountVnd,
    BigDecimal usedAmountVnd
) {

    public static SchoolSubscriptionQuotaUserAllocationDto toDto(SchoolSubscriptionQuotaUserAllocation domain, String fullName) {
        return new SchoolSubscriptionQuotaUserAllocationDto(
            domain.getId(), 
            domain.getUserId(),
            domain.getSchoolSubscriptionId(), 
            fullName,
            domain.getQuotaType().name(),
            domain.getAllocatedAmountVnd(),
            domain.getUsedAmountVnd()
        );
    }
}
