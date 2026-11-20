package com.sep.vox.application.port.input.query;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;

public record ViewAssessmentSchedulesQuery(
    UUID assessmentId,
    AssessmentScheduleStatus status,
    Instant startDate,
    Instant endDate
) {
}
