package com.sep.vox.application.port.input.command.assessmentpolicysystem;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateAssessmentPolicyCommand(
        UUID schoolId, // null for System Admin
        UUID policyId,
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        boolean passingScoreProvided,
        AssessmentPolicyStrictness strictness,
        boolean strictnessProvided,
        Instant effectiveFrom,
        Instant effectiveTo,
        boolean effectiveToProvided
) {}
