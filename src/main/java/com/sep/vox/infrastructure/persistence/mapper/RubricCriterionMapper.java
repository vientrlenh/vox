package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import com.sep.vox.infrastructure.persistence.entity.RubricCriterionJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RubricCriterionMapper {

    private RubricCriterionMapper() {}

    public static RubricCriterion toDomain(RubricCriterionJpaEntity jpa) {
        try {
            return RubricCriterion.builder()
                .id(jpa.getId())
                .rubricVersionId(jpa.getRubricVersionId())
                .frameworkCriterionId(jpa.getFrameworkCriterionId())
                .code(Code.from(jpa.getCode()))
                .name(jpa.getName())
                .description(jpa.getDescription())
                .examples(examplesFromJson(jpa.getExamplesJson()))
                .weight(PositiveDecimal.from(jpa.getWeight()))
                .minScore(PositiveDecimal.from(jpa.getMinScore()))
                .maxScore(PositiveDecimal.from(jpa.getMaxScore()))
                .required(jpa.getRequired())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric criterion entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }
    }

    public static RubricCriterionJpaEntity toJpa(RubricCriterion criterion) {
        try {
            return RubricCriterionJpaEntity.builder()
                .id(criterion.getId())
                .rubricVersionId(criterion.getRubricVersionId())
                .frameworkCriterionId(criterion.getFrameworkCriterionId())
                .code(Code.valueOf(criterion.getCode()))
                .name(criterion.getName())
                .description(criterion.getDescription())
                .examplesJson(examplesToJson(criterion.getExamples()))
                .weight(PositiveDecimal.valueOf(criterion.getWeight()))
                .minScore(PositiveDecimal.valueOf(criterion.getMinScore()))
                .maxScore(PositiveDecimal.valueOf(criterion.getMaxScore()))
                .required(criterion.getRequired())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping rubric criterion model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting model to entity");
        }
    }

    private static RubricCriterionExamples examplesFromJson(String json) {
        return json == null ? null : JsonValueObjectMapper.fromJson(json, RubricCriterionExamples.class);
    }

    private static String examplesToJson(RubricCriterionExamples examples) {
        return examples == null ? null : JsonValueObjectMapper.toJson(examples);
    }
}
