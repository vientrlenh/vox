package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;

public record AssessmentPolicyDto(
        UUID id,
        UUID schoolId,
        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,
        UUID languageId,
        UUID frameworkVersionId,
        UUID rubricVersionId,
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        String strictness,
        int version,
        AssessmentPolicyStatus status,
        Instant effectiveFrom,
        Instant effectiveTo,
        Instant createdAt,
        Instant updatedAt
) {}
