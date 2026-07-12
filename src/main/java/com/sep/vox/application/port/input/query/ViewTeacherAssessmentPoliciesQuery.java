package com.sep.vox.application.port.input.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ViewTeacherAssessmentPoliciesQuery(
        UUID languageId,
        UUID rubricVersionId,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo,
        int page,
        int size
) {}