package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteExamScheduleCommand(
    UUID examId,
    UUID scheduleId
) {
}
