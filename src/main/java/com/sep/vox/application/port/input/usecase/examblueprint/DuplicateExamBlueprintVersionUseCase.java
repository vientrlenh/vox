package com.sep.vox.application.port.input.usecase.examblueprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateExamBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.DuplicateExamBlueprintVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
public class DuplicateExamBlueprintVersionUseCase
        implements IUseCase<DuplicateExamBlueprintVersionCommand, ExamBlueprintVersionDto> {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final CreateExamBlueprintVersionUseCase createExamBlueprintVersionUseCase;

    public DuplicateExamBlueprintVersionUseCase(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            CreateExamBlueprintVersionUseCase createExamBlueprintVersionUseCase) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.createExamBlueprintVersionUseCase = createExamBlueprintVersionUseCase;
    }

    @Override
    @Transactional
    public ExamBlueprintVersionDto execute(DuplicateExamBlueprintVersionCommand input) {
        var sourceVersion = examBlueprintVersionRepository.findById(input.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var sections = examBlueprintSectionRepository.findByBlueprintVersionId(sourceVersion.getId()).stream()
            .map(section -> new CreateExamBlueprintSectionCommand(
                null,
                section.getOrder(),
                section.getTitle(),
                section.getInstruction(),
                section.getSectionTimeLimitSeconds(),
                section.getSectionWeight(),
                examBlueprintSlotRepository.findBySectionId(section.getId()).stream()
                    .map(this::toSlotCommand)
                    .toList()
            ))
            .toList();
        return createExamBlueprintVersionUseCase.execute(new CreateExamBlueprintVersionCommand(
            sourceVersion.getBlueprintId(),
            sourceVersion.getTotalTimeLimitSeconds(),
            null,
            null,
            sections
        ));
    }

    private CreateExamBlueprintSlotCommand toSlotCommand(ExamBlueprintSlot slot) {
        return new CreateExamBlueprintSlotCommand(
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
