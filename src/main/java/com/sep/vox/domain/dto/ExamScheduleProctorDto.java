package com.sep.vox.domain.dto;

import java.util.UUID;

public record ExamScheduleProctorDto(
    UUID id,
    UUID scheduleId,
    UUID teacherId
) {
}
