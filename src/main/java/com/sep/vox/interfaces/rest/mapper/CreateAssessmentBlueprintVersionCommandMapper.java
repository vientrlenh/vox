package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateAssessmentBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.AssessmentBlueprintSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.AssessmentBlueprintSlotRequest;
import com.sep.vox.interfaces.rest.dto.request.QuestionSelectionSpecRequest;

public final class CreateAssessmentBlueprintVersionCommandMapper {

    private CreateAssessmentBlueprintVersionCommandMapper() {
    }

    public static CreateAssessmentBlueprintVersionCommand fromRequest(UUID blueprintId, CreateAssessmentBlueprintVersionRequest request) {
        return new CreateAssessmentBlueprintVersionCommand(
            blueprintId,
            request.totalTimeLimitSeconds(),
            request.effectiveFrom(),
            request.effectiveTo(),
            toSections(request.sections())
        );
    }

    static List<CreateAssessmentBlueprintSectionCommand> toSections(List<AssessmentBlueprintSectionRequest> requests) {
        return requests.stream()
            .map(CreateAssessmentBlueprintVersionCommandMapper::toSection)
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
            request.slots().stream().map(CreateAssessmentBlueprintVersionCommandMapper::toSlot).toList()
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
