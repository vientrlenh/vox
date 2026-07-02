package com.sep.vox.interfaces.rest.mapper;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.CreateExamBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateExamBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateExamBlueprintVersionRequest;
import com.sep.vox.interfaces.rest.dto.request.ExamBlueprintSectionRequest;
import com.sep.vox.interfaces.rest.dto.request.ExamBlueprintSlotRequest;
import com.sep.vox.interfaces.rest.dto.request.QuestionSelectionSpecRequest;

public final class CreateExamBlueprintVersionCommandMapper {

    private CreateExamBlueprintVersionCommandMapper() {
    }

    public static CreateExamBlueprintVersionCommand fromRequest(UUID blueprintId, CreateExamBlueprintVersionRequest request) {
        return new CreateExamBlueprintVersionCommand(
            blueprintId,
            request.totalTimeLimitSeconds(),
            request.effectiveFrom(),
            request.effectiveTo(),
            toSections(request.sections())
        );
    }

    static List<CreateExamBlueprintSectionCommand> toSections(List<ExamBlueprintSectionRequest> requests) {
        return requests.stream()
            .map(CreateExamBlueprintVersionCommandMapper::toSection)
            .toList();
    }

    private static CreateExamBlueprintSectionCommand toSection(ExamBlueprintSectionRequest request) {
        return new CreateExamBlueprintSectionCommand(
            request.id(),
            request.order(),
            request.title(),
            request.instruction(),
            request.sectionTimeLimitSeconds(),
            request.sectionWeight(),
            request.slots().stream().map(CreateExamBlueprintVersionCommandMapper::toSlot).toList()
        );
    }

    private static CreateExamBlueprintSlotCommand toSlot(ExamBlueprintSlotRequest request) {
        return new CreateExamBlueprintSlotCommand(
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
