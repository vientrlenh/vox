package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolAssessmentPoliciesQuery(
        UUID schoolId,
        String status,
        UUID languageId,
        int page,
        int size
) {}
