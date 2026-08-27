package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanQuotaJpaEntity;

public final class SubscriptionPlanQuotaMapper {

    private SubscriptionPlanQuotaMapper() {}

    public static SubscriptionPlanQuota toDomain(SubscriptionPlanQuotaJpaEntity jpa) {
        return new SubscriptionPlanQuota(
            jpa.getId(),
            jpa.getSubscriptionPlanId(),
            fromString(jpa.getQuotaType()),
            jpa.getIncludedAmountVnd()
        );
    }

    public static SubscriptionPlanQuotaJpaEntity toJpa(SubscriptionPlanQuota domain) {
        return new SubscriptionPlanQuotaJpaEntity(
            domain.getId(),
            domain.getSubscriptionPlanId(),
            valueOf(domain.getQuotaType()),
            domain.getIncludedAmountVnd()
        );
    }

    private static QuotaType fromString(String type) {
        if (type == null) 
            return null;
        try {
            return QuotaType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại quota không hợp lệ khi chuyển đổi sang domain model: " + type);
        }
    }

    private static String valueOf(QuotaType type) {
        return type == null ? null : type.name();
    }
}
