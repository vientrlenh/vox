package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionBankStatusCommand(
    UUID id,
    String action
) {
}
