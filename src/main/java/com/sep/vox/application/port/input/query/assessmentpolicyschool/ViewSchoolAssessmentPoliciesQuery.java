package com.sep.vox.application.port.input.query.assessmentpolicyschool;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;

public record ViewSchoolAssessmentPoliciesQuery(
        UUID schoolId,
        AssessmentPolicyStatus status,
        UUID languageId,
        UUID rubricVersionId,
        Instant effectiveFrom,
        Instant effectiveTo,
        int page,
        int size
) {}
