package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

public record RubricCriterionDto(
        UUID id,
        UUID rubricVersionId,
        UUID frameworkCriterionId,
        String code,
        String name,
        String description,
        String examplesJson,
        BigDecimal weight,
        BigDecimal minScore,
        BigDecimal maxScore,
        int order,
        boolean isRequired,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID createdBy,
        UUID updatedBy
) {}