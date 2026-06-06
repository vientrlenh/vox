package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.infrastructure.persistence.entity.RubricCriterionJpaEntity;

public final class RubricCriterionMapper {

    private RubricCriterionMapper() {}

    public static RubricCriterion toDomain(RubricCriterionJpaEntity jpa) {
        return new RubricCriterion(
            jpa.getId(),
            jpa.getRubricVersionId(),
            jpa.getFrameworkCriterionId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getWeight(),
            jpa.getMinScore(),
            jpa.getMaxScore(),
            jpa.getOrder(),
            jpa.isRequired(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static RubricCriterionJpaEntity toJpa(RubricCriterion criterion) {
        return new RubricCriterionJpaEntity(
            criterion.getId(),
            criterion.getRubricVersionId(),
            criterion.getFrameworkCriterionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            criterion.getWeight(),
            criterion.getMinScore(),
            criterion.getMaxScore(),
            criterion.getOrder(),
            criterion.isRequired(),
            criterion.getCreatedAt(),
            criterion.getUpdatedAt(),
            criterion.getCreatedBy(),
            criterion.getUpdatedBy()
        );
    }
}
