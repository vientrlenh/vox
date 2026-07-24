package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssessmentPolicyDto(
        UUID id,
        UUID schoolId,
        UUID schoolGradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,
        UUID languageId,
        UUID frameworkVersionId,
        UUID rubricVersionId,
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        String strictness,
        int version,
        String status,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
