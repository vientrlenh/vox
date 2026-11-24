package com.sep.vox.application.port.input.command.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

public record UpdateAssessmentScheduleCommand(
    UUID id,
    UUID schoolRoomId,
    boolean schoolRoomIdProvided,
    Instant startDate,
    Instant endDate
) {
}
