package com.sep.vox.application.port.input.command.assessmentblueprint;

import java.util.UUID;

public record UpdateAssessmentBlueprintVersionStatusCommand(
    UUID versionId,
    String action,
    String note
) {
}
