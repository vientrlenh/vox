package com.sep.vox.interfaces.rest.dto.request.assessmentblueprint;

import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateQuestionSelectionSpecCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AssessmentBlueprintSectionRequest(
    UUID id,

    @NotNull(message = "Section order is required")
    Integer order,

    @NotBlank(message = "Section title is required")
    String title,

    String instruction,
    Integer sectionTimeLimitSeconds,
    BigDecimal sectionWeight,

    @Valid
    @NotEmpty(message = "A section must have at least one slot")
    List<AssessmentBlueprintSlotRequest> slots
) {

    public static List<CreateAssessmentBlueprintSectionCommand> toCommands(List<AssessmentBlueprintSectionRequest> requests) {
        return requests.stream()
            .map(AssessmentBlueprintSectionRequest::toSection)
            .toList();
    }

    private static CreateAssessmentBlueprintSectionCommand toSection(AssessmentBlueprintSectionRequest request) {
        return new CreateAssessmentBlueprintSectionCommand(
            request.id(),
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight(),
            request.slots().stream().map(AssessmentBlueprintSectionRequest::toSlot).toList()
        );
    }

    private static CreateAssessmentBlueprintSlotCommand toSlot(AssessmentBlueprintSlotRequest request) {
        return new CreateAssessmentBlueprintSlotCommand(
            request.id(),
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
