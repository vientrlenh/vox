package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateAssessmentBlueprintVersionCommand(
    UUID blueprintId,
    Integer totalTimeLimitSeconds,
    Instant effectiveFrom,
    Instant effectiveTo,
    List<CreateAssessmentBlueprintSectionCommand> sections
) {
}
