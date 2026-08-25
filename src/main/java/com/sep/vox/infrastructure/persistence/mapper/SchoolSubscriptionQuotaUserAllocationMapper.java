package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaUserAllocationJpaEntity;

public final class SchoolSubscriptionQuotaUserAllocationMapper {

    private SchoolSubscriptionQuotaUserAllocationMapper() {}

    public static SchoolSubscriptionQuotaUserAllocation toDomain(SchoolSubscriptionQuotaUserAllocationJpaEntity jpa) {
        return new SchoolSubscriptionQuotaUserAllocation(
            jpa.getId(),
            jpa.getSubscriptionId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getUserId(),
            jpa.getAllocatedQuantity(),
            jpa.getUsedQuantity()
        );
    }

    public static SchoolSubscriptionQuotaUserAllocationJpaEntity toJpa(SchoolSubscriptionQuotaUserAllocation domain) {
        return new SchoolSubscriptionQuotaUserAllocationJpaEntity(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getQuotaType().name(),
            domain.getUserId(),
            domain.getAllocatedQuantity(),
            domain.getUsedQuantity()
        );
    }
}
