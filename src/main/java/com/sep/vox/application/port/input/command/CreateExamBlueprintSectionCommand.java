package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateExamBlueprintSectionCommand(
    UUID id,
    int order,
    String title,
    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight,
    List<CreateExamBlueprintSlotCommand> slots
) {
}
