package com.sep.vox.domain.dto;

import java.util.UUID;

public record AssessmentScheduleProctorDto(
    UUID id,
    UUID scheduleId,
    UUID teacherId
) {
}
