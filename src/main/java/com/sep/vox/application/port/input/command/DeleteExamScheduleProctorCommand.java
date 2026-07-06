package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteExamScheduleProctorCommand(
    UUID examId,
    UUID scheduleId,
    UUID proctorId
) {
}
