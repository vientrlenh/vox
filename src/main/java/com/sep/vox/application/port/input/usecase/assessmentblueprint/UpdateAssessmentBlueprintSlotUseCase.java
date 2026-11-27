package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.question.Question;
import java.math.BigDecimal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSlotDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentBlueprintSlotUseCase implements IUseCase<UpdateAssessmentBlueprintSlotCommand, AssessmentBlueprintSlotDto> {

    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;
    private final QuestionRepository questionRepository;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;

    @Override
    @Transactional
    public AssessmentBlueprintSlotDto execute(UpdateAssessmentBlueprintSlotCommand input) {
        UpdateAssessmentBlueprintSlotCommand command = normalize(input);
        AssessmentBlueprintEditAccessService.EditableSlot editable =
            assessmentBlueprintEditAccessService.requireEditableSlot(command.slotId());
        AssessmentBlueprintSlot slot = editable.slot();
        AssessmentBlueprintVersion version = editable.version();
        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được sửa slot khi version đang DRAFT");
        }

        if (command.order() != slot.getOrder()) {
            List<AssessmentBlueprintSlot> siblings = assessmentBlueprintSlotRepository.findBySectionId(slot.getSectionId());
            if (siblings.stream().anyMatch(s -> !s.getId().equals(slot.getId()) && s.getOrder() == command.order())) {
                throw new IllegalArgumentException("Thứ tự slot đã tồn tại trong section này");
            }
        }
        validateSlot(command);

        applyChanges(slot, command);

        AssessmentBlueprintSlot savedSlot = assessmentBlueprintSlotRepository.save(slot);
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return AssessmentBlueprintSlotDtoMapper.toDto(savedSlot);
    }

    private void applyChanges(AssessmentBlueprintSlot slot, UpdateAssessmentBlueprintSlotCommand command) {
        slot.setOrder(command.order());
        slot.setWeight(command.weight() == null ? BigDecimal.ONE : command.weight());
        slot.setPrepTimeSecondsOverride(command.prepTimeSecondsOverride());
        slot.setResponseTimeSecondsOverride(command.responseTimeSecondsOverride());
        slot.setSlotType(command.slotType());
        slot.setFixedQuestionId(command.fixedQuestionId());
        slot.setSelectionSpec(selectionSpecOf(command.selectionSpec()));
    }

    private void validateSlot(UpdateAssessmentBlueprintSlotCommand command) {
        AssessmentBlueprintSlotType slotType = command.slotType();
        if (slotType == AssessmentBlueprintSlotType.FIXED) {
            if (command.fixedQuestionId() == null) {
                throw new IllegalArgumentException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (command.selectionSpec() != null) {
                throw new IllegalArgumentException("Slot FIXED không được có selectionSpec");
            }
            Question question = questionRepository.findById(command.fixedQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi fixed cho slot"));
            if (question.getStatus() != QuestionStatus.PUBLISHED) {
                throw new IllegalArgumentException("Câu hỏi fixed cho slot phải ở trạng thái PUBLISHED");
            }
            return;
        }
        if (command.fixedQuestionId() != null) {
            throw new IllegalArgumentException("Slot SELECTION không được có fixedQuestionId");
        }
        if (command.selectionSpec() == null) {
            throw new IllegalArgumentException("Slot SELECTION bắt buộc phải có selectionSpec");
        }
    }

    private UpdateAssessmentBlueprintSlotCommand normalize(UpdateAssessmentBlueprintSlotCommand input) {
        return new UpdateAssessmentBlueprintSlotCommand(
            input.slotId(),
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
