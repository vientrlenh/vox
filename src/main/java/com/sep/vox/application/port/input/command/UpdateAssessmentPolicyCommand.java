package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateAssessmentPolicyCommand(
        UUID schoolId, // null cho System Admin
        UUID policyId,
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        AssessmentPolicyStrictness strictness,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo
) {}
