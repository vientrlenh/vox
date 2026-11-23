package com.sep.vox.application.port.input.command.questionbank;

import java.util.UUID;

public record CreateQuestionBankGradeCommand(
    UUID questionBankId,
    UUID schoolGradeId
) {
}
