package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ChangeClassTestBlueprintCommand(
    UUID assessmentId,
    UUID blueprintId,
    UUID blueprintVersionId
) {
}
