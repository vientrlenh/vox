package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.question.QuestionBankStatus;

public record ReviewQuestionBankCommand(
    UUID bankId,
    QuestionBankStatus targetStatus
) {
}
