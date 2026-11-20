package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import java.time.Instant;
import java.util.UUID;

public record AssessmentScheduleDto(
    UUID id,
    UUID assessmentId,
    UUID schoolRoomId,
    Instant startDate,
    Instant endDate,
    AssessmentScheduleStatus status,
    UUID movedToScheduleId
) {
}
