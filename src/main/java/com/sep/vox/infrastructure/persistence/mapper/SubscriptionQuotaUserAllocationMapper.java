package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuotaUserAllocation;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionQuotaUserAllocationJpaEntity;

public final class SubscriptionQuotaUserAllocationMapper {

    private SubscriptionQuotaUserAllocationMapper() {}

    public static SubscriptionQuotaUserAllocation toDomain(SubscriptionQuotaUserAllocationJpaEntity jpa) {
        return new SubscriptionQuotaUserAllocation(
            jpa.getId(),
            jpa.getSubscriptionId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getUserId(),
            jpa.getAllocatedQuantity(),
            jpa.getUsedQuantity()
        );
    }

    public static SubscriptionQuotaUserAllocationJpaEntity toJpa(SubscriptionQuotaUserAllocation domain) {
        return new SubscriptionQuotaUserAllocationJpaEntity(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getQuotaType().name(),
            domain.getUserId(),
            domain.getAllocatedQuantity(),
            domain.getUsedQuantity()
        );
    }
}
