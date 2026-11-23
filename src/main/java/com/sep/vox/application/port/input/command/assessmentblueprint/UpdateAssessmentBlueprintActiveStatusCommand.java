package com.sep.vox.application.port.input.command.assessmentblueprint;

import java.util.UUID;

public record UpdateAssessmentBlueprintActiveStatusCommand(
    UUID blueprintId,
    boolean isActive
) {
}
