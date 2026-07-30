package com.sep.vox.domain.mapper;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public class RubricCriterionDtoMapper {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    public static RubricCriterionDto toDto(RubricCriterion criterion) {
        if (criterion == null) {
            return null;
        }
        return new RubricCriterionDto(
                criterion.getId(),
                criterion.getRubricVersionId(),
                criterion.getFrameworkCriterionId(),
                criterion.getCode(),
                criterion.getName(),
                criterion.getDescription(),
                toExamplesJson(criterion.getExamples()),
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

    public static List<RubricCriterionDto> toDtoList(List<RubricCriterion> criteria) {
        return criteria.stream()
                .map(RubricCriterionDtoMapper::toDto)
                .toList();
    }

    public static PageResult<RubricCriterionDto> toPage(PageResult<RubricCriterion> page) {
        return new PageResult<>(
                toDtoList(page.content()),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    private static String toExamplesJson(RubricCriterionExamples examples) {
        if (examples == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(examples);
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi serialize examplesJson", e);
        }
    }
}