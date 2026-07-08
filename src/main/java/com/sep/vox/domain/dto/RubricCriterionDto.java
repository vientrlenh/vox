package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

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