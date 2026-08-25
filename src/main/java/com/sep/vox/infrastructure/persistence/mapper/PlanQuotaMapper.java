package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.infrastructure.persistence.entity.PlanQuotaJpaEntity;

public final class PlanQuotaMapper {

    private PlanQuotaMapper() {}

    public static PlanQuota toDomain(PlanQuotaJpaEntity jpa) {
        return new PlanQuota(
            jpa.getId(),
            jpa.getPlanId(),
            QuotaType.valueOf(jpa.getQuotaType()),
            jpa.getIncludedQuantity(),
            jpa.getTokenUnitPrice()
        );
    }

    public static PlanQuotaJpaEntity toJpa(PlanQuota domain) {
        return new PlanQuotaJpaEntity(
            domain.getId(),
            domain.getPlanId(),
            domain.getQuotaType().name(),
            domain.getIncludedQuantity(),
            domain.getTokenUnitPrice()
        );
    }
}
