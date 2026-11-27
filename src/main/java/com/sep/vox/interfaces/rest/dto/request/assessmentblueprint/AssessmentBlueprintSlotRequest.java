package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AssessmentBlueprintSlotRequest(
    UUID id,

    @NotNull(message = "Slot order is required")
    Integer order,

    BigDecimal weight,
    Integer prepTimeSecondsOverride,
    Integer responseTimeSecondsOverride,

    @NotNull(message = "Slot type is required")
    AssessmentBlueprintSlotType slotType,

    UUID fixedQuestionId,

    @Valid
    QuestionSelectionSpecRequest selectionSpec
) {

    @AssertTrue(message = "A FIXED slot must have fixedQuestionId and no selectionSpec; a SELECTION slot must have selectionSpec and no fixedQuestionId")
    public boolean isSlotConfigValid() {
        if (slotType == null) {
            return true;
        }
        if (slotType == AssessmentBlueprintSlotType.FIXED) {
            return fixedQuestionId != null && selectionSpec == null;
        }
        return fixedQuestionId == null && selectionSpec != null;
    }
}
