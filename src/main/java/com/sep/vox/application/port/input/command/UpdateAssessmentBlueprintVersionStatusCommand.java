package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateAssessmentBlueprintVersionStatusCommand(
    UUID versionId,
    String action,
    String note
) {
}
