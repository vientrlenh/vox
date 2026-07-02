package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamBlueprintVersionStatusCommand(
    UUID versionId,
    String action,
    String note
) {
}
