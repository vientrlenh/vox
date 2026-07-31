package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

public record CreateExamScheduleCommand(
    UUID examId,
    UUID schoolRoomId,
    Instant startDate,
    Instant endDate
) {
}
