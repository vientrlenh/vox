package com.sep.vox.interfaces.graphql.dto.request;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAssessmentPolicyInput(
        UUID targetFrameworkBandId,
        BigDecimal passingScore,
        AssessmentPolicyStrictness strictness,
        String effectiveFrom,
        String effectiveTo
) {}
