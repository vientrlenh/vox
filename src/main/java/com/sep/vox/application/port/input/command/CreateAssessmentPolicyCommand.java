package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAssessmentPolicyCommand(
        UUID schoolId,
        UUID frameworkVersionId,
        UUID rubricVersionId,
        UUID languageId,
        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        AssessmentPolicyStrictness strictness,
        Instant effectiveFrom,
        Instant effectiveTo
) {}