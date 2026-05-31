package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubriclevelmapping.RubricLevelMapping;
import com.sep.vox.infrastructure.persistence.entity.RubricLevelMappingJpaEntity;

public final class RubricLevelMappingMapper {

    private RubricLevelMappingMapper() {}

    public static RubricLevelMapping toDomain(RubricLevelMappingJpaEntity jpa) {
        return new RubricLevelMapping(
            jpa.getId(),
            jpa.getRubricVersionId(),
            jpa.getStandardLevelVersionId(),
            jpa.getScoreMin(),
            jpa.getScoreMax(),
            jpa.getDescription(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static RubricLevelMappingJpaEntity toJpa(RubricLevelMapping mapping) {
        return new RubricLevelMappingJpaEntity(
            mapping.getId(),
            mapping.getRubricVersionId(),
            mapping.getStandardLevelVersionId(),
            mapping.getScoreMin(),
            mapping.getScoreMax(),
            mapping.getDescription(),
            mapping.getCreatedAt(),
            mapping.getUpdatedAt(),
            mapping.getCreatedBy(),
            mapping.getUpdatedBy()
        );
    }
}
