package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateExamBlueprintSectionItemCommand(
    UUID versionId,
    int order,
    String title,
    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight
) {
}
