package com.sep.vox.application.port.input.command.assessmentschedule;

import java.util.UUID;

public record DeleteAssessmentScheduleCommand(
    UUID assessmentId,
    UUID scheduleId
) {
}
