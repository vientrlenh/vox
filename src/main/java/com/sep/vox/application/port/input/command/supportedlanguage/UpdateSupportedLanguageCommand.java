package com.sep.vox.application.port.input.command.supportedlanguage;

import java.util.UUID;

public record UpdateSupportedLanguageCommand(
    UUID id,
    String code,
    boolean codeProvided,
    String name,
    boolean nameProvided,
    String description,
    boolean descriptionProvided,
    Boolean isActive,
    boolean isActiveProvided
) {
}
