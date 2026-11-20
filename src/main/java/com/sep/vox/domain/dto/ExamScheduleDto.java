package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamScheduleDto(
    UUID id,
    UUID examId,
    UUID schoolRoomId,
    String startDate,
    String endDate,
    String status,
    UUID movedToScheduleId
) {
}
