package com.sep.vox.interfaces.rest.dto.request.assessment;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintSlotCommand;
import com.sep.vox.interfaces.rest.dto.request.QuestionSelectionSpecRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateAssessmentBlueprintSlotRequest(
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

    public static UpdateAssessmentBlueprintSlotCommand toCommand(UUID slotId, UpdateAssessmentBlueprintSlotRequest request) {
        return new UpdateAssessmentBlueprintSlotCommand(
            slotId,
            request.order(),
            request.weight(),
            request.prepTimeSecondsOverride(),
            request.responseTimeSecondsOverride(),
            request.slotType(),
            request.fixedQuestionId(),
            toSelectionSpec(request.selectionSpec())
        );
    }

    private static CreateQuestionSelectionSpecCommand toSelectionSpec(QuestionSelectionSpecRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateQuestionSelectionSpecCommand(
            request.questionType(),
            request.difficulty(),
            request.targetBandLevel(),
            request.skillCode(),
            request.topicId()
        );
    }
}
