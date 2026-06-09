package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionStatus;

public record ReviewQuestionCommand(
    UUID questionId,
    QuestionStatus targetStatus,
    String note,
    String reason
) {
}
