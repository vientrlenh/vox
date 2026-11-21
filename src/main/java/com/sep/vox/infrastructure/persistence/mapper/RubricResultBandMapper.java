package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricResultBandJpaEntity;

public final class RubricResultBandMapper {
    
    public static RubricResultBand toDomain(RubricResultBandJpaEntity jpa) {
        return RubricResultBand.builder()
                .id(jpa.getId())
                .rubricVersionId(jpa.getRubricVersionId())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .scoreMin(jpa.getScoreMin())
                .scoreMax(jpa.getScoreMax())
                .order(jpa.getOrder())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static RubricResultBandJpaEntity toJpa(RubricResultBand band) {
        return RubricResultBandJpaEntity.builder()
                .id(band.getId())
                .rubricVersionId(band.getRubricVersionId())
                .code(band.getCode())
                .name(band.getName())
                .description(band.getDescription())
                .scoreMin(band.getScoreMin())
                .scoreMax(band.getScoreMax())
                .order(band.getOrder())
                .createdAt(band.getCreatedAt())
                .updatedAt(band.getUpdatedAt())
                .createdBy(band.getCreatedBy())
                .updatedBy(band.getUpdatedBy())
                .build();
    }
}
