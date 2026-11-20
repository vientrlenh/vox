package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentKind;

public record ViewAssessmentStatusCountsQuery(
    UUID schoolId,
    AssessmentKind kind,
    Instant createdFrom,
    Instant createdTo
) {
}
