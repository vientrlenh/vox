package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAssessmentPolicyCommand(
        UUID schoolId,
        UUID frameworkVersionId,
        List<UUID> rubricVersionIds,
        UUID languageId,
        UUID schoolGradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,
        UUID targetFrameworkBandId,
        UUID minimumFrameworkBandId,
        BigDecimal passingScore,
        AssessmentPolicyStrictness strictness,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo
) {}