package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateAssessmentBlueprintActiveStatusCommand(
    UUID blueprintId,
    boolean isActive
) {
}
