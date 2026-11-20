package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSlotItemCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintSlotItemRequest;
import com.sep.vox.interfaces.rest.dto.request.QuestionSelectionSpecRequest;

public final class CreateAssessmentBlueprintSlotCommandMapper {

    private CreateAssessmentBlueprintSlotCommandMapper() {
    }

    public static CreateAssessmentBlueprintSlotItemCommand fromRequest(UUID sectionId, CreateAssessmentBlueprintSlotItemRequest request) {
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
