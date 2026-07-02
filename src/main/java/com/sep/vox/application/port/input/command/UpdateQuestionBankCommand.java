package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateQuestionBankCommand(
    UUID id,
    String name,
    String description
) {
}
