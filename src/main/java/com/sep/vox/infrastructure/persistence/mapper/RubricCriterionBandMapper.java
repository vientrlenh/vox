package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricCriterionBand;
import com.sep.vox.domain.valueobject.framework.RubricCriterionSignals;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionBandExamples;
import com.sep.vox.infrastructure.persistence.entity.RubricCriterionBandJpaEntity;

public final class RubricCriterionBandMapper {

    private RubricCriterionBandMapper() {}

    public static RubricCriterionBand toDomain(RubricCriterionBandJpaEntity jpa) {
        return new RubricCriterionBand(
            jpa.getId(),
            jpa.getCriterionId(),
            jpa.getCode(),
            jpa.getScoreMin(),
            jpa.getScoreMax(),
            jpa.getDescriptor(),
            JsonValueObjectMapper.fromJson(jpa.getPositiveSignalsJson(), RubricCriterionSignals.class),
            JsonValueObjectMapper.fromJson(jpa.getNegativeSignalsJson(), RubricCriterionSignals.class),
            JsonValueObjectMapper.fromJson(jpa.getExamplesJson(), RubricCriterionBandExamples.class),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static RubricCriterionBandJpaEntity toJpa(RubricCriterionBand band) {
        return new RubricCriterionBandJpaEntity(
            band.getId(),
            band.getCriterionId(),
            band.getCode(),
            band.getScoreMin(),
            band.getScoreMax(),
            band.getDescriptor(),
            JsonValueObjectMapper.toJson(band.getPositiveSignals()),
            JsonValueObjectMapper.toJson(band.getNegativeSignals()),
            JsonValueObjectMapper.toJson(band.getExamples()),
            band.getCreatedAt(),
            band.getUpdatedAt(),
            band.getCreatedBy(),
            band.getUpdatedBy()
        );
    }
}
