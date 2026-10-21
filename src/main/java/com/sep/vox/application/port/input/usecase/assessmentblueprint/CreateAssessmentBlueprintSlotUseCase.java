package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSlotItemCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.assessment.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.question.QuestionDifficulty;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
public class CreateAssessmentBlueprintSlotUseCase implements IUseCase<CreateAssessmentBlueprintSlotItemCommand, AssessmentBlueprintSlotDto> {

    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    public CreateAssessmentBlueprintSlotUseCase(
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService,
            UserContextPort userContextPort) {
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.recalculateBlueprintVersionTimeLimitService = recalculateBlueprintVersionTimeLimitService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentBlueprintSlotDto execute(CreateAssessmentBlueprintSlotItemCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var section = assessmentBlueprintSectionRepository.findById(command.sectionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy section"));
        var version = assessmentBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        var blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm slot khi version đang DRAFT");
        }

        var siblings = assessmentBlueprintSlotRepository.findBySectionId(section.getId());
        if (siblings.stream().anyMatch(s -> s.getOrder() == command.order())) {
            throw new IllegalStateException("Thứ tự slot đã tồn tại trong section này");
        }
        validateSlot(command);

        var now = Instant.now();
        var slot = new AssessmentBlueprintSlot(
            section.getId(),
            version.getId(),
            command.order(),
            command.weight() == null ? BigDecimal.ONE : command.weight(),
            command.prepTimeSecondsOverride(),
            command.responseTimeSecondsOverride(),
            AssessmentBlueprintSlotType.valueOf(command.slotType()),
            command.fixedQuestionId(),
            selectionSpecOf(command.selectionSpec()),
            now,
            now,
            currentUserId,
            currentUserId
        );
        var savedSlot = assessmentBlueprintSlotRepository.save(slot);
        recalculateBlueprintVersionTimeLimitService.recalculate(version.getId());
        return AssessmentBlueprintSlotDto.toDto(savedSlot);
    }

    private void validateSlot(CreateAssessmentBlueprintSlotItemCommand command) {
        var slotType = AssessmentBlueprintSlotType.valueOf(command.slotType());
        if (slotType == AssessmentBlueprintSlotType.FIXED) {
            if (command.fixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (command.selectionSpec() != null) {
                throw new IllegalStateException("Slot FIXED không được có selectionSpec");
            }
            var question = questionRepository.findById(command.fixedQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi fixed cho slot"));
            if (question.getStatus() != QuestionStatus.PUBLISHED) {
                throw new IllegalStateException("Câu hỏi fixed cho slot phải ở trạng thái PUBLISHED");
            }
            return;
        }
        if (command.fixedQuestionId() != null) {
            throw new IllegalStateException("Slot SELECTION không được có fixedQuestionId");
        }
        if (command.selectionSpec() == null) {
            throw new IllegalStateException("Slot SELECTION bắt buộc phải có selectionSpec");
        }
    }

    private CreateAssessmentBlueprintSlotItemCommand normalize(CreateAssessmentBlueprintSlotItemCommand input) {
        return new CreateAssessmentBlueprintSlotItemCommand(
            input.sectionId(),
            input.order(),
            input.weight(),
            input.prepTimeSecondsOverride(),
            input.responseTimeSecondsOverride(),
            StringNormalization.normalizeCode(input.slotType()),
            input.fixedQuestionId(),
            normalizeSelectionSpec(input.selectionSpec())
        );
    }

    private CreateQuestionSelectionSpecCommand normalizeSelectionSpec(CreateQuestionSelectionSpecCommand input) {
        if (input == null) {
            return null;
        }
        return new CreateQuestionSelectionSpecCommand(
            StringNormalization.normalizeCode(input.questionType()),
            StringNormalization.normalizeCode(input.difficulty()),
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
            input.questionType() == null ? null : QuestionType.valueOf(input.questionType()),
            input.difficulty() == null ? null : QuestionDifficulty.valueOf(input.difficulty()),
            input.targetBandLevel(),
            input.skillCode(),
            input.topicId()
        );
    }
}
