package com.sep.vox.domain.dto;

import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricCriterionDto(
        UUID id,
        UUID rubricVersionId,
        UUID frameworkCriterionId,
        String code,
        String name,
        String description,
        RubricCriterionExamples examplesJson,
        BigDecimal weight,
        BigDecimal minScore,
        BigDecimal maxScore,
        int order,
        boolean isRequired
) {}