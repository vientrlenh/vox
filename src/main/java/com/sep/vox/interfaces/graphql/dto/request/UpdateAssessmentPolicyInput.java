package com.sep.vox.interfaces.graphql.dto.request;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateAssessmentPolicyInput(
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        AssessmentPolicyStrictness strictness,
        Instant effectiveFrom,
        Instant effectiveTo
) {}
