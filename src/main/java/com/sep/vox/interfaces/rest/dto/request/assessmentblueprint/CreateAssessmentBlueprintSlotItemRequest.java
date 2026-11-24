package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSlotItemCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateQuestionSelectionSpecCommand;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreateAssessmentBlueprintSlotItemRequest(
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

    public static CreateAssessmentBlueprintSlotItemCommand toCommand(UUID sectionId, CreateAssessmentBlueprintSlotItemRequest request) {
        return new CreateAssessmentBlueprintSlotItemCommand(
            sectionId,
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
