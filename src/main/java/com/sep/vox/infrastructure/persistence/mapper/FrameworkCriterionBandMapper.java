package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionBandJpaEntity;

public final class FrameworkCriterionBandMapper {

    private FrameworkCriterionBandMapper() {}

    public static FrameworkCriterionBand toDomain(FrameworkCriterionBandJpaEntity jpa) {
        return new FrameworkCriterionBand(
            jpa.getId(),
            jpa.getFrameworkCriterionId(),
            jpa.getFrameworkResultBandId(),
            jpa.getDescriptor(),
            jpa.getPositiveSignalsJson() == null ? null : JsonValueObjectMapper.fromJson(jpa.getPositiveSignalsJson(), FrameworkCriterionSignals.class),
            jpa.getNegativeSignalsJson() == null ? null : JsonValueObjectMapper.fromJson(jpa.getNegativeSignalsJson(), FrameworkCriterionSignals.class),
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
            JsonValueObjectMapper.toJson(band.getPositiveSignals()),
            JsonValueObjectMapper.toJson(band.getNegativeSignals()),
            band.getCreatedAt(),
            band.getUpdatedAt(),
            band.getCreatedBy(),
            band.getUpdatedBy()
        );
    }
}
