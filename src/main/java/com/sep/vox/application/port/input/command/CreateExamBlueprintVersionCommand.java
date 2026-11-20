package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateExamBlueprintVersionCommand(
    UUID blueprintId,
    Integer totalTimeLimitSeconds,
    String effectiveFrom,
    String effectiveTo,
    List<CreateExamBlueprintSectionCommand> sections
) {
}
