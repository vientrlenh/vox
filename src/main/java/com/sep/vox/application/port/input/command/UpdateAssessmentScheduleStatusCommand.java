package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateAssessmentScheduleStatusCommand(
    UUID assessmentId,
    UUID scheduleId,
    String action,
    String note,
    UUID targetScheduleId
) {
}
