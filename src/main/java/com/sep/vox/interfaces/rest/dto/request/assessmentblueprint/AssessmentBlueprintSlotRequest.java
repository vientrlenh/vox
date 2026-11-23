package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AssessmentBlueprintSlotRequest(
    UUID id,

    @NotNull(message = "Thứ tự slot là bắt buộc")
    Integer order,

    BigDecimal weight,
    Integer prepTimeSecondsOverride,
    Integer responseTimeSecondsOverride,

    @NotNull(message = "Loại slot là bắt buộc")
    AssessmentBlueprintSlotType slotType,

    UUID fixedQuestionId,

    @Valid
    QuestionSelectionSpecRequest selectionSpec
) {

    @AssertTrue(message = "Slot FIXED phải có fixedQuestionId và không có selectionSpec; slot SELECTION phải có selectionSpec và không có fixedQuestionId")
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
