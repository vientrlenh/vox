package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionJpaEntity;

public final class FrameworkCriterionMapper {

    private FrameworkCriterionMapper() {}

    public static FrameworkCriterion toDomain(FrameworkCriterionJpaEntity jpa) {
        return new FrameworkCriterion(
            jpa.getId(),
            jpa.getFrameworkVersionId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static FrameworkCriterionJpaEntity toJpa(FrameworkCriterion criterion) {
        return new FrameworkCriterionJpaEntity(
            criterion.getId(),
            criterion.getFrameworkVersionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            criterion.getCreatedAt(),
            criterion.getUpdatedAt(),
            criterion.getCreatedBy(),
            criterion.getUpdatedBy()
        );
    }
}
