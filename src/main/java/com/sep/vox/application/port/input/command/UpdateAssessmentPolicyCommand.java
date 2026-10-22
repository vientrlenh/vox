package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateAssessmentPolicyCommand(
        UUID schoolId, // null cho System Admin
        UUID policyId,
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        Instant effectiveFrom,
        Instant effectiveTo
) {}
