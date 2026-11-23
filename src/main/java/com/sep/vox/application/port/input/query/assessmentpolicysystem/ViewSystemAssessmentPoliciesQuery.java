package com.sep.vox.application.port.input.query.assessmentpolicysystem;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;

public record ViewSystemAssessmentPoliciesQuery(
        AssessmentPolicyStatus status,
        UUID languageId,
        UUID rubricVersionId,
        Instant effectiveFrom,
        Instant effectiveTo,
        int page,
        int size
) {}
