package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;

public record UpdateAssessmentBlueprintSlotCommand(
    UUID slotId,
    int order,
    BigDecimal weight,
    Integer prepTimeSecondsOverride,
    Integer responseTimeSecondsOverride,
    AssessmentBlueprintSlotType slotType,
    UUID fixedQuestionId,
    CreateQuestionSelectionSpecCommand selectionSpec
) {
}
