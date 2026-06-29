package com.sep.vox.application.port.input.usecase.examblueprint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSlotItemCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.mapper.ExamBlueprintSlotDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.question.QuestionDifficulty;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
public class CreateExamBlueprintSlotUseCase implements IUseCase<CreateExamBlueprintSlotItemCommand, ExamBlueprintSlotDto> {

    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamMemberRepository examMemberRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateExamBlueprintSlotUseCase(
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamMemberRepository examMemberRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examMemberRepository = examMemberRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintSlotDto execute(CreateExamBlueprintSlotItemCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var section = examBlueprintSectionRepository.findById(command.sectionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy section"));
        var version = examBlueprintVersionRepository.findById(section.getBlueprintVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        authorizeAuthor(currentUserId, currentSchoolId, blueprint.getSchoolId());

        if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được thêm slot khi version đang DRAFT");
        }

        var siblings = examBlueprintSlotRepository.findBySectionId(section.getId());
        if (siblings.stream().anyMatch(s -> s.getOrder() == command.order())) {
            throw new IllegalStateException("Thứ tự slot đã tồn tại trong section này");
        }
        validateSlot(command);

        var now = OffsetDateTime.now();
        var slot = new ExamBlueprintSlot(
            section.getId(),
            version.getId(),
            command.order(),
            command.weight() == null ? BigDecimal.ONE : command.weight(),
            command.prepTimeSecondsOverride(),
            command.responseTimeSecondsOverride(),
            ExamBlueprintSlotType.valueOf(command.slotType()),
            command.fixedQuestionId(),
            selectionSpecOf(command.selectionSpec()),
            now,
            now,
            currentUserId,
            currentUserId
        );
        return ExamBlueprintSlotDtoMapper.toDto(examBlueprintSlotRepository.save(slot));
    }

    private void validateSlot(CreateExamBlueprintSlotItemCommand command) {
        var slotType = ExamBlueprintSlotType.valueOf(command.slotType());
        if (slotType == ExamBlueprintSlotType.FIXED) {
            if (command.fixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (command.selectionSpec() != null) {
                throw new IllegalStateException("Slot FIXED không được có selectionSpec");
            }
            if (!questionRepository.existsById(command.fixedQuestionId())) {
                throw new NotFoundException("Không tìm thấy câu hỏi fixed cho slot");
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

    private void authorizeAuthor(java.util.UUID currentUserId, java.util.UUID currentSchoolId, java.util.UUID schoolId) {
        if (!schoolId.equals(currentSchoolId)
                || !examMemberRepository.existsByUserIdAndRoleAndSchoolId(currentUserId, ExamMemberRole.AUTHOR, schoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private CreateExamBlueprintSlotItemCommand normalize(CreateExamBlueprintSlotItemCommand input) {
        return new CreateExamBlueprintSlotItemCommand(
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
