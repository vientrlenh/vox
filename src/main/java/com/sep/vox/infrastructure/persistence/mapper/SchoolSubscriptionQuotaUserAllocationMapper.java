package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionQuotaUserAllocationJpaEntity;

public final class SchoolSubscriptionQuotaUserAllocationMapper {

    private SchoolSubscriptionQuotaUserAllocationMapper() {}

    public static SchoolSubscriptionQuotaUserAllocation toDomain(SchoolSubscriptionQuotaUserAllocationJpaEntity jpa) {
        return SchoolSubscriptionQuotaUserAllocation.builder()
            .id(jpa.getId())
            .schoolSubscriptionId(jpa.getSchoolSubscriptionId())
            .quotaType(fromString(jpa.getQuotaType()))
            .userId(jpa.getUserId())
            .allocatedAmountVnd(jpa.getAllocatedAmountVnd())
            .usedAmountVnd(jpa.getUsedAmountVnd())
            .build();
    }

    public static SchoolSubscriptionQuotaUserAllocationJpaEntity toJpa(SchoolSubscriptionQuotaUserAllocation domain) {
        return SchoolSubscriptionQuotaUserAllocationJpaEntity.builder()
            .id(domain.getId())
            .schoolSubscriptionId(domain.getSchoolSubscriptionId())
            .quotaType(valueOf(domain.getQuotaType()))
            .userId(domain.getUserId())
            .allocatedAmountVnd(domain.getAllocatedAmountVnd())
            .usedAmountVnd(domain.getUsedAmountVnd())
            .build();
    }

    private static QuotaType fromString(String type) {
        if (type == null) 
            return null;
        try {
            return QuotaType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid quota type when converting to the domain model: " + type);
        }
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
