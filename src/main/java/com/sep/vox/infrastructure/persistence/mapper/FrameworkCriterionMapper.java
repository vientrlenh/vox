package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkCriterionJpaEntity;

public final class FrameworkCriterionMapper {

    private FrameworkCriterionMapper() {}

    public static FrameworkCriterion toDomain(FrameworkCriterionJpaEntity jpa) {
        return FrameworkCriterion.builder()
            .id(jpa.getId())
            .frameworkVersionId(jpa.getFrameworkVersionId())
            .code(jpa.getCode())
            .name(jpa.getName())
            .description(jpa.getDescription())
            .order(jpa.getOrder())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static FrameworkCriterionJpaEntity toJpa(FrameworkCriterion criterion) {
        return FrameworkCriterionJpaEntity.builder()
            .id(criterion.getId())
            .frameworkVersionId(criterion.getFrameworkVersionId())
            .code(criterion.getCode())
            .name(criterion.getName())
            .description(criterion.getDescription())
            .order(criterion.getOrder())
            .createdAt(criterion.getCreatedAt())
            .updatedAt(criterion.getUpdatedAt())
            .createdBy(criterion.getCreatedBy())
            .updatedBy(criterion.getUpdatedBy())
            .build();
    }
}
