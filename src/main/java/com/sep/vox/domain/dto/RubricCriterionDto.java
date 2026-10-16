package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

public record RubricCriterionDto(
    UUID id,
    UUID rubricVersionId,
    UUID frameworkCriterionId,
    String code,
    String name,
    String description,
    RubricCriterionExamples examples,
    BigDecimal weight,
    BigDecimal minScore,
    BigDecimal maxScore,
    Boolean required
) {

    public static RubricCriterionDto toDto(RubricCriterion rubricCriterion) {
        return new RubricCriterionDto(
            rubricCriterion.getId(),
            rubricCriterion.getRubricVersionId(),
            rubricCriterion.getFrameworkCriterionId(),
            Code.valueOf(rubricCriterion.getCode()),
            rubricCriterion.getName(),
            rubricCriterion.getDescription(),
            rubricCriterion.getExamples(),
            PositiveDecimal.valueOf(rubricCriterion.getWeight()),
            PositiveDecimal.valueOf(rubricCriterion.getMinScore()),
            PositiveDecimal.valueOf(rubricCriterion.getMaxScore()),
            rubricCriterion.getRequired()
        );
    }

    public static List<RubricCriterionDto> toDtoList(List<RubricCriterion> criteria) {
        return criteria.stream()
                .map(RubricCriterionDto::toDto)
                .toList();
    }

    public static PageResult<RubricCriterionDto> toDtoPage(PageResult<RubricCriterion> page) {
        return new PageResult<>(
                toDtoList(page.content()),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}
