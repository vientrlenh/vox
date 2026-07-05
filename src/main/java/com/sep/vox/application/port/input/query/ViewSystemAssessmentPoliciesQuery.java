package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSystemAssessmentPoliciesQuery(
        String status,
        UUID languageId,
        int page,
        int size
) {}
