package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.infrastructure.persistence.entity.RubricJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RubricMapper {

    private RubricMapper() {}

    public static Rubric toDomain(RubricJpaEntity jpa) {
        try {
            return Rubric.builder()
                .id(jpa.getId())
                .learningLanguageId(jpa.getLearningLanguageId())
                .frameworkId(jpa.getFrameworkId())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .ownerType(RubricOwnerType.from(jpa.getOwnerType()))
                .schoolId(jpa.getSchoolId())
                .createdAt(jpa.getCreatedAt())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static RubricJpaEntity toJpa(Rubric rubric) {
        try {
            return RubricJpaEntity.builder()
                .id(rubric.getId())
                .learningLanguageId(rubric.getLearningLanguageId())
                .frameworkId(rubric.getFrameworkId())
                .name(rubric.getName())
                .description(rubric.getDescription())
                .ownerType(RubricOwnerType.value(rubric.getOwnerType()))
                .schoolId(rubric.getSchoolId())
                .createdAt(rubric.getCreatedAt())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }
}
