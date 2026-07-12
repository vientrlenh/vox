package com.sep.vox.application.port.input.usecase.examblueprint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintSlotCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.mapper.ExamBlueprintSlotDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.question.QuestionDifficulty;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
public class UpdateExamBlueprintSlotUseCase implements IUseCase<UpdateExamBlueprintSlotCommand, ExamBlueprintSlotDto> {

    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public UpdateExamBlueprintSlotUseCase(
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintSlotDto execute(UpdateExamBlueprintSlotCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var slot = examBlueprintSlotRepository.findById(command.slotId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy slot"));
        var version = examBlueprintVersionRepository.findById(slot.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!examBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được sửa slot khi version đang DRAFT");
        }

        if (command.order() != slot.getOrder()) {
            var siblings = examBlueprintSlotRepository.findBySectionId(slot.getSectionId());
            if (siblings.stream().anyMatch(s -> !s.getId().equals(slot.getId()) && s.getOrder() == command.order())) {
                throw new IllegalStateException("Thứ tự slot đã tồn tại trong section này");
            }
        }
        validateSlot(command);

        slot.setOrder(command.order());
        slot.setWeight(command.weight() == null ? BigDecimal.ONE : command.weight());
        slot.setPrepTimeSecondsOverride(command.prepTimeSecondsOverride());
        slot.setResponseTimeSecondsOverride(command.responseTimeSecondsOverride());
        slot.setSlotType(ExamBlueprintSlotType.valueOf(command.slotType()));
        slot.setFixedQuestionId(command.fixedQuestionId());
        slot.setSelectionSpec(selectionSpecOf(command.selectionSpec()));
        slot.setUpdatedAt(OffsetDateTime.now());
        slot.setUpdatedBy(currentUserId);

        return ExamBlueprintSlotDtoMapper.toDto(examBlueprintSlotRepository.save(slot));
    }

    private void validateSlot(UpdateExamBlueprintSlotCommand command) {
        var slotType = ExamBlueprintSlotType.valueOf(command.slotType());
        if (slotType == ExamBlueprintSlotType.FIXED) {
            if (command.fixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (command.selectionSpec() != null) {
                throw new IllegalStateException("Slot FIXED không được có selectionSpec");
            }
            var question = questionRepository.findById(command.fixedQuestionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi fixed cho slot"));
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

    private UpdateExamBlueprintSlotCommand normalize(UpdateExamBlueprintSlotCommand input) {
        return new UpdateExamBlueprintSlotCommand(
            input.slotId(),
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
