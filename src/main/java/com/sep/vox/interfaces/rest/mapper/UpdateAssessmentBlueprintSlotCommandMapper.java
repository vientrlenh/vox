package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentBlueprintSlotCommand;
import com.sep.vox.interfaces.rest.dto.request.QuestionSelectionSpecRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateAssessmentBlueprintSlotRequest;

public final class UpdateAssessmentBlueprintSlotCommandMapper {

    private UpdateAssessmentBlueprintSlotCommandMapper() {
    }

    public static UpdateAssessmentBlueprintSlotCommand fromRequest(UUID slotId, UpdateAssessmentBlueprintSlotRequest request) {
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
