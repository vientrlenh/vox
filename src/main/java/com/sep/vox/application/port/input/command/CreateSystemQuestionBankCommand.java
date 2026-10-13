package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSystemQuestionBankCommand(
    UUID learningLanguageId,
    String name,
    String description
) {
}
