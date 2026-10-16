package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;
import com.sep.vox.infrastructure.persistence.entity.RubricResultBandJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RubricResultBandMapper {

    private RubricResultBandMapper() {}

    public static RubricResultBand toDomain(RubricResultBandJpaEntity jpa) {
        try {
            return RubricResultBand.builder()
                .id(jpa.getId())
                .rubricVersionId(jpa.getRubricVersionId())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .scoreMin(PositiveDecimal.from(jpa.getScoreMin()))
                .scoreMax(PositiveDecimal.from(jpa.getScoreMax()))
                .order(PositiveInteger.from(jpa.getOrder()))
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric result band entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static RubricResultBandJpaEntity toJpa(RubricResultBand band) {
        try {
            return RubricResultBandJpaEntity.builder()
                .id(band.getId())
                .rubricVersionId(band.getRubricVersionId())
                .name(band.getName())
                .description(band.getDescription())
                .scoreMin(PositiveDecimal.valueOf(band.getScoreMin()))
                .scoreMax(PositiveDecimal.valueOf(band.getScoreMax()))
                .order(PositiveInteger.valueOf(band.getOrder()))
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric result band model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
