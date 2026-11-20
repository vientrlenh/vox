package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ChangeClassTestBlueprintCommand(
    UUID examId,
    UUID blueprintId,
    UUID blueprintVersionId
) {
}
