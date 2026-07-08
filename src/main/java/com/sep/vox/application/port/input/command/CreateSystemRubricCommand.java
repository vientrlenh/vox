package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSystemRubricCommand(
        String code,
        String name,
        String description,
        UUID languageId,
        UUID frameworkId
) {
}