package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionBandJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FrameworkCriterionBandMapper {

    private FrameworkCriterionBandMapper() {}

    public static FrameworkCriterionBand toDomain(FrameworkCriterionBandJpaEntity jpa) {
        try {
            return FrameworkCriterionBand.builder()
                .id(jpa.getId())
                .frameworkCriterionId(jpa.getFrameworkCriterionId())
                .frameworkResultBandId(jpa.getFrameworkResultBandId())
                .descriptor(jpa.getDescriptor())
                .positiveSignals(signalsFromJson(jpa.getPositiveSignalsJson()))
                .negativeSignals(signalsFromJson(jpa.getNegativeSignalsJson()))
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework criterion band entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static FrameworkCriterionBandJpaEntity toJpa(FrameworkCriterionBand band) {
        try {
            return FrameworkCriterionBandJpaEntity.builder()
                .id(band.getId())
                .frameworkCriterionId(band.getFrameworkCriterionId())
                .frameworkResultBandId(band.getFrameworkResultBandId())
                .descriptor(band.getDescriptor())
                .positiveSignalsJson(signalsToJson(band.getPositiveSignals()))
                .negativeSignalsJson(signalsToJson(band.getNegativeSignals()))
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework criterion band model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }

    private static FrameworkCriterionSignals signalsFromJson(String json) {
        return json == null ? null : JsonValueObjectMapper.fromJson(json, FrameworkCriterionSignals.class);
    }

    private static String signalsToJson(FrameworkCriterionSignals signals) {
        return signals == null ? null : JsonValueObjectMapper.toJson(signals);
    }
}
