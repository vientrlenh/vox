package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkCriterionBandJpaEntity;

public final class FrameworkCriterionBandMapper {

    private FrameworkCriterionBandMapper() {}

    public static FrameworkCriterionBand toDomain(FrameworkCriterionBandJpaEntity jpa) {
        return FrameworkCriterionBand.builder()
            .id(jpa.getId())
            .frameworkCriterionId(jpa.getFrameworkCriterionId())
            .frameworkResultBandId(jpa.getFrameworkResultBandId())
            .descriptor(jpa.getDescriptor())
            .positiveSignals(jpa.getPositiveSignalsJson() == null ? null : JsonValueObjectMapper.fromJson(jpa.getPositiveSignalsJson(), FrameworkCriterionSignals.class))
            .negativeSignals(jpa.getNegativeSignalsJson() == null ? null : JsonValueObjectMapper.fromJson(jpa.getNegativeSignalsJson(), FrameworkCriterionSignals.class))
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static FrameworkCriterionBandJpaEntity toJpa(FrameworkCriterionBand band) {
        return FrameworkCriterionBandJpaEntity.builder()
            .id(band.getId())
            .frameworkCriterionId(band.getFrameworkCriterionId())
            .frameworkResultBandId(band.getFrameworkResultBandId())
            .descriptor(band.getDescriptor())
            .positiveSignalsJson(JsonValueObjectMapper.toJson(band.getPositiveSignals()))
            .negativeSignalsJson(JsonValueObjectMapper.toJson(band.getNegativeSignals()))
            .createdAt(band.getCreatedAt())
            .updatedAt(band.getUpdatedAt())
            .createdBy(band.getCreatedBy())
            .updatedBy(band.getUpdatedBy())
            .build();
    }
}
