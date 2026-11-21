package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import com.sep.vox.infrastructure.persistence.entity.rubric.RubricCriterionJpaEntity;

public final class RubricCriterionMapper {

    private RubricCriterionMapper() {}

    public static RubricCriterion toDomain(RubricCriterionJpaEntity jpa) {
        return RubricCriterion.builder()
                .id(jpa.getId())
                .rubricVersionId(jpa.getRubricVersionId())
                .frameworkCriterionId(jpa.getFrameworkCriterionId())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .examples(jpa.getExamplesJson() == null ? null : JsonValueObjectMapper.fromJson(jpa.getExamplesJson(), RubricCriterionExamples.class))
                .weight(jpa.getWeight())
                .minScore(jpa.getMinScore())
                .maxScore(jpa.getMaxScore())
                .order(jpa.getOrder())
                .required(jpa.isRequired())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static RubricCriterionJpaEntity toJpa(RubricCriterion criterion) {
        return RubricCriterionJpaEntity.builder()
                .id(criterion.getId())
                .rubricVersionId(criterion.getRubricVersionId())
                .frameworkCriterionId(criterion.getFrameworkCriterionId())
                .code(criterion.getCode())
                .name(criterion.getName())
                .description(criterion.getDescription())
                .examplesJson(JsonValueObjectMapper.toJson(criterion.getExamples()))
                .weight(criterion.getWeight())
                .minScore(criterion.getMinScore())
                .maxScore(criterion.getMaxScore())
                .order(criterion.getOrder())
                .isRequired(criterion.isRequired())
                .createdAt(criterion.getCreatedAt())
                .updatedAt(criterion.getUpdatedAt())
                .createdBy(criterion.getCreatedBy())
                .updatedBy(criterion.getUpdatedBy())
                .build();
    }
}
