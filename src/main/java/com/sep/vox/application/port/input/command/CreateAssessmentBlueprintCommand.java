package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateAssessmentBlueprintCommand(
    UUID languageId,
    UUID gradeLevelId,
    String code,
    String name,
    String description
) {
}
