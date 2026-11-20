package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

public record UpdateExamScheduleCommand(
    UUID id,
    UUID schoolRoomId,
    Instant startDate,
    Instant endDate
) {
}
