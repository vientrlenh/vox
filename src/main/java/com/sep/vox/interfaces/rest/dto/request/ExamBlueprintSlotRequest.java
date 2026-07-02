package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ExamBlueprintSlotRequest(
    UUID id,

    @NotNull(message = "Thứ tự slot là bắt buộc")
    Integer order,

    BigDecimal weight,
    Integer prepTimeSecondsOverride,
    Integer responseTimeSecondsOverride,

    @NotNull(message = "Loại slot là bắt buộc")
    @Pattern(regexp = "FIXED|SELECTION", message = "Loại slot không hợp lệ")
    String slotType,

    UUID fixedQuestionId,

    @Valid
    QuestionSelectionSpecRequest selectionSpec
) {

    @AssertTrue(message = "Slot FIXED phải có fixedQuestionId và không có selectionSpec; slot SELECTION phải có selectionSpec và không có fixedQuestionId")
    public boolean isSlotConfigValid() {
        if (slotType == null) {
            return true;
        }
        if ("FIXED".equals(slotType)) {
            return fixedQuestionId != null && selectionSpec == null;
        }
        return fixedQuestionId == null && selectionSpec != null;
    }
}
