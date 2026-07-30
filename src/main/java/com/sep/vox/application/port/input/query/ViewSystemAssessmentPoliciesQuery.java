package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

public record ViewSystemAssessmentPoliciesQuery(
        String status,
        UUID languageId,
        UUID rubricVersionId,
        Instant effectiveFrom,
        Instant effectiveTo,
        int page,
        int size
) {}
