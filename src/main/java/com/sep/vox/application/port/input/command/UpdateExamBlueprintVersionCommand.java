package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record UpdateExamBlueprintVersionCommand(
    UUID versionId,
    String description,
    Integer totalTimeLimitSeconds,
    String effectiveFrom,
    String effectiveTo,
    List<CreateExamBlueprintSectionCommand> sections
) {
}
