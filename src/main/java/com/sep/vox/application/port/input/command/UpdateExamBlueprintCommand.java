package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamBlueprintCommand(
    UUID blueprintId,
    String name,
    String description
) {
}
