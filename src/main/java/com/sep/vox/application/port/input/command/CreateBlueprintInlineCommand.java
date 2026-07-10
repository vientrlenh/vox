package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateBlueprintInlineCommand(
    String code,
    String name,
    String description,
    UUID schoolGradeLevelId,
    UUID languageId
) {
}
