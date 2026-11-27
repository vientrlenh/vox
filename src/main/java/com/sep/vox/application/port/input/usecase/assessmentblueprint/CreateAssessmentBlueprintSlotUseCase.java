package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.question.Question;
import java.math.BigDecimal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSlotItemCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSlotDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class CreateAssessmentBlueprintSlotUseCase implements IUseCase<CreateAssessmentBlueprintSlotItemCommand, AssessmentBlueprintSlotDto> {

    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;

    @Override
    @Transactional
    public AssessmentBlueprintSlotDto execute(CreateAssessmentBlueprintSlotItemCommand input) {
        CreateAssessmentBlueprintSlotItemCommand command = normalize(input);
        AssessmentBlueprintEditAccessService.EditableSection editable =
            assessmentBlueprintEditAccessService.requireEditableSection(command.sectionId());
        AssessmentBlueprintSection section = editable.section();
        AssessmentBlueprintVersion version = editable.version();
        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("A slot can only be added while the version is DRAFT");
        }

        List<AssessmentBlueprintSlot> siblings = assessmentBlueprintSlotRepository.findBySectionId(section.getId());
        if (siblings.stream().anyMatch(s -> s.getOrder() == command.order())) {
            throw new IllegalArgumentException("The slot order already exists in this section");
        }
        validateSlot(command);

        AssessmentBlueprintSlot savedSlot = assessmentBlueprintSlotRepository.save(newSlot(command, section, version));
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return AssessmentBlueprintSlotDtoMapper.toDto(savedSlot);
    }

    private AssessmentBlueprintSlot newSlot(
            CreateAssessmentBlueprintSlotItemCommand command,
            AssessmentBlueprintSection section,
            AssessmentBlueprintVersion version) {
        return AssessmentBlueprintSlot.create(
            section.getId(),
            version.getId(),
            command.order(),
            command.weight() == null ? BigDecimal.ONE : command.weight(),
            command.prepTimeSecondsOverride(),
            command.responseTimeSecondsOverride(),
            command.slotType(),
            command.fixedQuestionId(),
            selectionSpecOf(command.selectionSpec()));
    }

    private void validateSlot(CreateAssessmentBlueprintSlotItemCommand command) {
        AssessmentBlueprintSlotType slotType = command.slotType();
        if (slotType == AssessmentBlueprintSlotType.FIXED) {
            if (command.fixedQuestionId() == null) {
                throw new IllegalArgumentException("A FIXED slot must have a fixedQuestionId");
            }
            if (command.selectionSpec() != null) {
                throw new IllegalArgumentException("A FIXED slot must not have a selectionSpec");
            }
            Question question = questionRepository.findById(command.fixedQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Fixed question for the slot not found"));
            if (question.getStatus() != QuestionStatus.PUBLISHED) {
                throw new IllegalArgumentException("The fixed question of the slot must be PUBLISHED");
            }
            return;
        }
        if (command.fixedQuestionId() != null) {
            throw new IllegalArgumentException("A SELECTION slot must not have a fixedQuestionId");
        }
        if (command.selectionSpec() == null) {
            throw new IllegalArgumentException("A SELECTION slot must have a selectionSpec");
        }
    }

    private CreateAssessmentBlueprintSlotItemCommand normalize(CreateAssessmentBlueprintSlotItemCommand input) {
        return new CreateAssessmentBlueprintSlotItemCommand(
            input.sectionId(),
            input.order(),
            input.weight(),
            input.prepTimeSecondsOverride(),
            input.responseTimeSecondsOverride(),
            input.slotType(),
            input.fixedQuestionId(),
            normalizeSelectionSpec(input.selectionSpec())
        );
    }

    private CreateQuestionSelectionSpecCommand normalizeSelectionSpec(CreateQuestionSelectionSpecCommand input) {
        if (input == null) {
            return null;
        }
        return new CreateQuestionSelectionSpecCommand(
            input.questionType(),
            input.difficulty(),
            StringNormalization.trimAndCollapseSpaces(input.targetBandLevel()),
            StringNormalization.trimAndCollapseSpaces(input.skillCode()),
            input.topicId()
        );
    }

    private QuestionSelectionSpec selectionSpecOf(CreateQuestionSelectionSpecCommand input) {
        if (input == null) {
            return null;
        }
        return new QuestionSelectionSpec(
            input.questionType(),
            input.difficulty(),
            input.targetBandLevel(),
            input.skillCode(),
            input.topicId()
        );
    }
}
