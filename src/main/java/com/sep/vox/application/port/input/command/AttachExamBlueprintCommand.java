package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record AttachExamBlueprintCommand(
    UUID examId,
    UUID blueprintId,
    UUID blueprintVersionId,
    CreateBlueprintInlineCommand newBlueprint
) {
}
