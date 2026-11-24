package com.sep.vox.application.port.input.command.assessmentblueprint;

import java.util.UUID;

public record UpdateAssessmentBlueprintCommand(
    UUID blueprintId,
    String name,
    String description
) {
}
