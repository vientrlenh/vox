package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateExamScheduleCommand(
    UUID examId,
    UUID schoolRoomId,
    OffsetDateTime startDate,
    OffsetDateTime endDate
) {
}
