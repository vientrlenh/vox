package com.sep.vox.application.port.input.command.assessmentblueprint;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAssessmentBlueprintSectionCommand(
    UUID sectionId,
    int order,
    String title,
    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight
) {
}
