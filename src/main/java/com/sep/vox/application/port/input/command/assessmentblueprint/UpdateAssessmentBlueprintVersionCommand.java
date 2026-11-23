package com.sep.vox.application.port.input.command.assessmentblueprint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateAssessmentBlueprintVersionCommand(
    UUID versionId,
    String description,
    Integer totalTimeLimitSeconds,
    Instant effectiveFrom,
    Instant effectiveTo,
    List<CreateAssessmentBlueprintSectionCommand> sections
) {
}
