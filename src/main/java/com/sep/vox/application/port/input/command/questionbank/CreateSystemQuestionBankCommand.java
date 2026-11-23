package com.sep.vox.application.port.input.command.questionbank;

import java.util.UUID;

public record CreateSystemQuestionBankCommand(
    UUID languageId,
    String code,
    String name,
    String description
) {
}
