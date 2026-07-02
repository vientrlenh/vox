package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateExamBlueprintSlotCommand(
    UUID slotId,
    int order,
    BigDecimal weight,
    Integer prepTimeSecondsOverride,
    Integer responseTimeSecondsOverride,
    String slotType,
    UUID fixedQuestionId,
    CreateQuestionSelectionSpecCommand selectionSpec
) {
}
