package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamScheduleStatusCommand(
    UUID examId,
    UUID scheduleId,
    String action,
    String note,
    UUID targetScheduleId
) {
}
