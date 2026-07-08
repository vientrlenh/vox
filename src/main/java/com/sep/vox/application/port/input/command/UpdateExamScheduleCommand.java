package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateExamScheduleCommand(
    UUID id,
    UUID schoolRoomId,
    OffsetDateTime startDate,
    OffsetDateTime endDate
) {
}
