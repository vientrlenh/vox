package com.sep.vox.application.port.input.command.assessmentschedule;

import java.util.UUID;

public record DeleteAssessmentScheduleProctorCommand(
    UUID assessmentId,
    UUID scheduleId,
    UUID proctorId
) {
}
