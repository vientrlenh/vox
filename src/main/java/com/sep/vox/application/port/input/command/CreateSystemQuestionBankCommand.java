package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSystemQuestionBankCommand(
    UUID languageId,
    String code,
    String name,
    String description
) {
}
