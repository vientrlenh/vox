package com.sep.vox.application.port.input.command.supportedlanguage;

public record CreateSupportedLanguageCommand(
    String code,
    String name,
    String description
) {
}
