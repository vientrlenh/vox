package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionQuotaJpaEntity;

public final class SubscriptionQuotaMapper {

    private SubscriptionQuotaMapper() {}

    public static SubscriptionQuota toDomain(SubscriptionQuotaJpaEntity jpa) {
        return new SubscriptionQuota(
            jpa.getId(),
            jpa.getSubscriptionId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getTotalAllocated(),
            jpa.getUsedQuantity()
        );
    }

    public static SubscriptionQuotaJpaEntity toJpa(SubscriptionQuota domain) {
        return new SubscriptionQuotaJpaEntity(
            domain.getId(),
            domain.getSubscriptionId(),
            domain.getQuotaType().name(),
            domain.getTotalAllocated(),
            domain.getUsedQuantity()
        );
    }
}
