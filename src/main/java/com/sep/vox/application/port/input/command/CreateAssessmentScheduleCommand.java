package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

public record CreateAssessmentScheduleCommand(
    UUID assessmentId,
    UUID schoolRoomId,
    Instant startDate,
    Instant endDate
) {
}
