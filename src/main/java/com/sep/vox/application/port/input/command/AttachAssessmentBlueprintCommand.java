package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AttachAssessmentBlueprintCommand(
    UUID assessmentId,
    UUID blueprintId,
    UUID blueprintVersionId,
    CreateBlueprintInlineCommand newBlueprint
) {
}
