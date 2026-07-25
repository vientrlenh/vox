package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSessionStatus;

public record UpdateExamSessionStatusCommand(
    UUID sessionId,
    ExamSessionStatus status
) {
}
