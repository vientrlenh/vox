package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.question.Question;
import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSlotItemCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintSlotDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class CreateAssessmentBlueprintSlotUseCase implements IUseCase<CreateAssessmentBlueprintSlotItemCommand, AssessmentBlueprintSlotDto> {

    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentBlueprintSlotDto execute(CreateAssessmentBlueprintSlotItemCommand input) {
        CreateAssessmentBlueprintSlotItemCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprintSection section = assessmentBlueprintSectionRepository.findById(command.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy section"));
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được thêm slot khi version đang DRAFT");
        }

        List<AssessmentBlueprintSlot> siblings = assessmentBlueprintSlotRepository.findBySectionId(section.getId());
        if (siblings.stream().anyMatch(s -> s.getOrder() == command.order())) {
            throw new IllegalArgumentException("Thứ tự slot đã tồn tại trong section này");
        }
        validateSlot(command);

        AssessmentBlueprintSlot slot = AssessmentBlueprintSlot.builder()
            .sectionId(section.getId())
            .blueprintVersionId(version.getId())
            .order(command.order())
            .weight(command.weight() == null ? BigDecimal.ONE : command.weight())
            .prepTimeSecondsOverride(command.prepTimeSecondsOverride())
            .responseTimeSecondsOverride(command.responseTimeSecondsOverride())
            .slotType(command.slotType())
            .fixedQuestionId(command.fixedQuestionId())
            .selectionSpec(selectionSpecOf(command.selectionSpec()))
            .build();
        AssessmentBlueprintSlot savedSlot = assessmentBlueprintSlotRepository.save(slot);
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return AssessmentBlueprintSlotDtoMapper.toDto(savedSlot);
    }

    private void validateSlot(CreateAssessmentBlueprintSlotItemCommand command) {
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
