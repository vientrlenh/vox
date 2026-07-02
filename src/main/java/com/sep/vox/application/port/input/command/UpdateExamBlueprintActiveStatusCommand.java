package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamBlueprintActiveStatusCommand(
    UUID blueprintId,
    boolean isActive
) {
}
