package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.infrastructure.persistence.entity.FrameworkCriterionJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FrameworkCriterionMapper {

    private FrameworkCriterionMapper() {}

    public static FrameworkCriterion toDomain(FrameworkCriterionJpaEntity jpa) {
        try {
            return FrameworkCriterion.builder()
                .id(jpa.getId())
                .frameworkVersionId(jpa.getFrameworkVersionId())
                .code(Code.from(jpa.getCode()))
                .name(jpa.getName())
                .description(jpa.getDescription())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework criterion entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static FrameworkCriterionJpaEntity toJpa(FrameworkCriterion criterion) {
        try {
            return FrameworkCriterionJpaEntity.builder()
                .id(criterion.getId())
                .frameworkVersionId(criterion.getFrameworkVersionId())
                .code(Code.valueOf(criterion.getCode()))
                .name(criterion.getName())
                .description(criterion.getDescription())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping framework criterion model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
