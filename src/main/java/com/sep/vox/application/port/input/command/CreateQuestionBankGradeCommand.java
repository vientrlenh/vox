package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateQuestionBankGradeCommand(
    UUID questionBankId,
    UUID schoolGradeId
) {
}
