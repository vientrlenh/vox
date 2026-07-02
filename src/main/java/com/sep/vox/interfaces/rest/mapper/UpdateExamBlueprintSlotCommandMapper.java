package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintSlotCommand;
import com.sep.vox.interfaces.rest.dto.request.QuestionSelectionSpecRequest;
import com.sep.vox.interfaces.rest.dto.request.UpdateExamBlueprintSlotRequest;

public final class UpdateExamBlueprintSlotCommandMapper {

    private UpdateExamBlueprintSlotCommandMapper() {
    }

    public static UpdateExamBlueprintSlotCommand fromRequest(UUID slotId, UpdateExamBlueprintSlotRequest request) {
        return new UpdateExamBlueprintSlotCommand(
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
