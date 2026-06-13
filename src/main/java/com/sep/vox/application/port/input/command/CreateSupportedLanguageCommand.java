package com.sep.vox.application.port.input.command;

public record CreateSupportedLanguageCommand(
    String code,
    String name,
    String description
) {
}
