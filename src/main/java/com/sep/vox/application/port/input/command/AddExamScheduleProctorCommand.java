package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AddExamScheduleProctorCommand(
    UUID examId,
    UUID scheduleId,
    UUID teacherId
) {
}
