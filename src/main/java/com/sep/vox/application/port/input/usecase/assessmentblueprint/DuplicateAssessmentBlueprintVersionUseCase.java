package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.DuplicateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class DuplicateAssessmentBlueprintVersionUseCase
        implements IUseCase<DuplicateAssessmentBlueprintVersionCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final CreateAssessmentBlueprintVersionUseCase createAssessmentBlueprintVersionUseCase;

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(DuplicateAssessmentBlueprintVersionCommand input) {
        AssessmentBlueprintVersion sourceVersion = assessmentBlueprintVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        List<CreateAssessmentBlueprintSectionCommand> sections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(sourceVersion.getId()).stream()
            .map(section -> new CreateAssessmentBlueprintSectionCommand(
                null,
                section.getOrder(),
                section.getTitle(),
                section.getInstruction(),
                section.getSectionTimeLimitSeconds(),
                section.getSectionWeight(),
                assessmentBlueprintSlotRepository.findBySectionId(section.getId()).stream()
                    .map(this::toSlotCommand)
                    .toList()
            ))
            .toList();
        return createAssessmentBlueprintVersionUseCase.execute(new CreateAssessmentBlueprintVersionCommand(
            sourceVersion.getBlueprintId(),
            sourceVersion.getTotalTimeLimitSeconds(),
            null,
            null,
            sections
        ));
    }

    private CreateAssessmentBlueprintSlotCommand toSlotCommand(AssessmentBlueprintSlot slot) {
        return new CreateAssessmentBlueprintSlotCommand(
            null,
            slot.getOrder(),
            slot.getWeight(),
            slot.getPrepTimeSecondsOverride(),
            slot.getResponseTimeSecondsOverride(),
            slot.getSlotType(),
            slot.getFixedQuestionId(),
            toSelectionSpecCommand(slot.getSelectionSpec())
        );
    }

    private CreateQuestionSelectionSpecCommand toSelectionSpecCommand(QuestionSelectionSpec selectionSpec) {
        if (selectionSpec == null) {
            return null;
        }
        return new CreateQuestionSelectionSpecCommand(
            selectionSpec.questionType(),
            selectionSpec.difficulty(),
            selectionSpec.targetBandLevel(),
            selectionSpec.skillCode(),
            selectionSpec.topicId()
        );
    }
}
