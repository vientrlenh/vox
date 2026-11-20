package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteAssessmentScheduleProctorCommand(
    UUID assessmentId,
    UUID scheduleId,
    UUID proctorId
) {
}
