package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewSystemAssessmentPoliciesQuery(
        String status,
        UUID languageId,
        UUID rubricVersionId,
        Instant effectiveFrom,
        Instant effectiveTo,
        AppPageRequest pageRequest
) {}
