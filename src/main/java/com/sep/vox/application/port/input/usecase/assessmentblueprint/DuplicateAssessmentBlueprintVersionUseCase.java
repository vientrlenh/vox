package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.DuplicateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
public class DuplicateAssessmentBlueprintVersionUseCase
        implements IUseCase<DuplicateAssessmentBlueprintVersionCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final CreateAssessmentBlueprintVersionUseCase createAssessmentBlueprintVersionUseCase;

    public DuplicateAssessmentBlueprintVersionUseCase(
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            CreateAssessmentBlueprintVersionUseCase createAssessmentBlueprintVersionUseCase) {
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.createAssessmentBlueprintVersionUseCase = createAssessmentBlueprintVersionUseCase;
    }

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(DuplicateAssessmentBlueprintVersionCommand input) {
        var sourceVersion = assessmentBlueprintVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        var sections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(sourceVersion.getId()).stream()
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
            slot.getSlotType().name(),
            slot.getFixedQuestionId(),
            toSelectionSpecCommand(slot.getSelectionSpec())
        );
    }

    private CreateQuestionSelectionSpecCommand toSelectionSpecCommand(QuestionSelectionSpec selectionSpec) {
        if (selectionSpec == null) {
            return null;
        }
        return new CreateQuestionSelectionSpecCommand(
            selectionSpec.questionType() == null ? null : selectionSpec.questionType().name(),
            selectionSpec.difficulty() == null ? null : selectionSpec.difficulty().name(),
            selectionSpec.targetBandLevel(),
            selectionSpec.skillCode(),
            selectionSpec.topicId()
        );
    }
}
