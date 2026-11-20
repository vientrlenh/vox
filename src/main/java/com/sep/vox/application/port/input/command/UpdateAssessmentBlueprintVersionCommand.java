package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record UpdateAssessmentBlueprintVersionCommand(
    UUID versionId,
    String description,
    Integer totalTimeLimitSeconds,
    String effectiveFrom,
    String effectiveTo,
    List<CreateAssessmentBlueprintSectionCommand> sections
) {
}
