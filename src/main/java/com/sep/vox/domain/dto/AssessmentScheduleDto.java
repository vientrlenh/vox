package com.sep.vox.domain.dto;

import java.util.UUID;

public record AssessmentScheduleDto(
    UUID id,
    UUID assessmentId,
    UUID schoolRoomId,
    String startDate,
    String endDate,
    String status,
    UUID movedToScheduleId
) {
}
