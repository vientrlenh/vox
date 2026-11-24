package com.sep.vox.application.port.input.command.questionbank;

import java.util.UUID;

public record UpdateQuestionBankStatusCommand(
    UUID id,
    String action
) {
}
