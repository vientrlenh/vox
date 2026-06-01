package com.sep.vox.application.port.input.command;

public record CreateQuestionBankCommand(
    String bankName,
    String description
) {
}
