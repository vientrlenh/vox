package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AssignExamCandidateScheduleCommand(
    UUID examId,
    UUID candidateId,
    UUID scheduleId
) {
}
