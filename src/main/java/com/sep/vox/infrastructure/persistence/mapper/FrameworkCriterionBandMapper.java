package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionBandJpaEntity;

public final class FrameworkCriterionBandMapper {

    private FrameworkCriterionBandMapper() {}

    public static FrameworkCriterionBand toDomain(FrameworkCriterionBandJpaEntity jpa) {
        return new FrameworkCriterionBand(
            jpa.getId(),
            jpa.getFrameworkCriterionId(),
            jpa.getFrameworkResultBandId(),
            jpa.getDescriptor(),
            jpa.getPositiveSignals(),
            jpa.getNegativeSignals(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static FrameworkCriterionBandJpaEntity toJpa(FrameworkCriterionBand band) {
        return new FrameworkCriterionBandJpaEntity(
            band.getId(),
            band.getFrameworkCriterionId(),
            band.getFrameworkResultBandId(),
            band.getDescriptor(),
            band.getPositiveSignals(),
            band.getNegativeSignals(),
            band.getCreatedAt(),
            band.getUpdatedAt(),
            band.getCreatedBy(),
            band.getUpdatedBy()
        );
    }
}
