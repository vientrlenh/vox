package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
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
            JsonValueObjectMapper.fromJson(normalizeExamplesJson(jpa.getExamplesJson()), RubricCriterionExamples.class),
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

    /**
     * Dữ liệu cũ có thể đã bị lưu sai dạng mảng thô ([...]) thay vì đúng shape
     * record RubricCriterionExamples ({"values": [...]}). Bọc lại nếu phát hiện dạng mảng.
     */
    private static String normalizeExamplesJson(String examplesJson) {
        if (examplesJson == null) {
            return null;
        }
        String trimmed = examplesJson.strip();
        return trimmed.startsWith("[") ? "{\"values\":" + trimmed + "}" : examplesJson;
    }

    public static RubricCriterionJpaEntity toJpa(RubricCriterion criterion) {
        return new RubricCriterionJpaEntity(
            criterion.getId(),
            criterion.getRubricVersionId(),
            criterion.getFrameworkCriterionId(),
            criterion.getCode(),
            criterion.getName(),
            criterion.getDescription(),
            JsonValueObjectMapper.toJson(criterion.getExamples()),
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
