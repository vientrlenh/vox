package com.sep.vox.application.port.input.command.assessmentschedule;

import java.util.UUID;

public record AddAssessmentScheduleProctorCommand(
    UUID assessmentId,
    UUID scheduleId,
    UUID teacherId
) {
}
