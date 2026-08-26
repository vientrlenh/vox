package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;

public record SchoolSubscriptionQuotaUserAllocationDto(
    UUID id, 
    UUID userId,
    UUID schoolSubscriptionId, 
    String quotaType,
    BigDecimal allocatedAmountVnd,
    BigDecimal usedAmountVnd
) {

    public static SchoolSubscriptionQuotaUserAllocationDto toDto(SchoolSubscriptionQuotaUserAllocation domain) {
        return new SchoolSubscriptionQuotaUserAllocationDto(
            domain.getId(), 
            domain.getUserId(),
            domain.getSchoolSubscriptionId(), 
            valueOf(domain.getQuotaType()),
            domain.getAllocatedAmountVnd(),
            domain.getUsedAmountVnd()
        );
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
