package com.sep.vox.application.port.input.usecase.examblueprint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateExamBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
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
public class CreateExamBlueprintVersionUseCase implements IUseCase<CreateExamBlueprintVersionCommand, ExamBlueprintVersionDto> {

    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final ExamMemberRepository examMemberRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public CreateExamBlueprintVersionUseCase(
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            ExamMemberRepository examMemberRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            UserContextPort userContextPort) {
        this.examBlueprintRepository = examBlueprintRepository;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.examMemberRepository = examMemberRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintVersionDto execute(CreateExamBlueprintVersionCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var blueprint = examBlueprintRepository.findById(command.blueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        authorizeAuthor(currentUserId, currentSchoolId, blueprint.getSchoolId());

        validateSections(command);

        var now = OffsetDateTime.now();
        var versionNumber = examBlueprintVersionRepository.nextVersionNumber(blueprint.getId());
        var version = new ExamBlueprintVersion(
            blueprint.getId(),
            versionNumber,
            blueprint.getCode() + "-V" + versionNumber,
            null,
            ExamBlueprintVersionStatus.DRAFT,
            command.totalTimeLimitSeconds(),
            effectiveFromOf(command.effectiveFrom(), now),
            parseDateTime(command.effectiveTo()),
            now,
            now,
            currentUserId,
            currentUserId
        );
        var savedVersion = examBlueprintVersionRepository.save(version);

        for (var sectionCommand : command.sections()) {
            var savedSection = examBlueprintSectionRepository.save(new ExamBlueprintSection(
                savedVersion.getId(),
                sectionCommand.order(),
                sectionCommand.title(),
                sectionCommand.instruction(),
                sectionCommand.sectionTimeLimitSeconds(),
                defaultWeight(sectionCommand.sectionWeight()),
                now,
                now,
                currentUserId,
                currentUserId
            ));
            for (var slotCommand : slotsOf(sectionCommand)) {
                examBlueprintSlotRepository.save(new ExamBlueprintSlot(
                    savedSection.getId(),
                    savedVersion.getId(),
                    slotCommand.order(),
                    defaultWeight(slotCommand.weight()),
                    slotCommand.prepTimeSecondsOverride(),
                    slotCommand.responseTimeSecondsOverride(),
                    ExamBlueprintSlotType.valueOf(slotCommand.slotType()),
                    slotCommand.fixedQuestionId(),
                    selectionSpecOf(slotCommand.selectionSpec()),
                    now,
                    now,
                    currentUserId,
                    currentUserId
                ));
            }
        }

        return ExamBlueprintVersionDtoMapper.toDto(savedVersion);
    }

    private CreateExamBlueprintVersionCommand normalize(CreateExamBlueprintVersionCommand input) {
        return new CreateExamBlueprintVersionCommand(
            input.blueprintId(),
            input.totalTimeLimitSeconds(),
            input.effectiveFrom(),
            input.effectiveTo(),
            input.sections() == null ? java.util.List.of() : input.sections().stream()
                .map(this::normalizeSection)
                .toList()
        );
    }

    private CreateExamBlueprintSectionCommand normalizeSection(CreateExamBlueprintSectionCommand input) {
        return new CreateExamBlueprintSectionCommand(
            input.id(),
            input.order(),
            StringNormalization.trimAndCollapseSpaces(input.title()),
            StringNormalization.trimAndCollapseSpaces(input.instruction()),
            input.sectionTimeLimitSeconds(),
            input.sectionWeight(),
            input.slots() == null ? java.util.List.of() : input.slots().stream()
                .map(this::normalizeSlot)
                .toList()
        );
    }

    private CreateExamBlueprintSlotCommand normalizeSlot(CreateExamBlueprintSlotCommand input) {
        return new CreateExamBlueprintSlotCommand(
            input.id(),
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

    private void authorizeAuthor(UUID currentUserId, UUID currentSchoolId, UUID schoolId) {
        if (!schoolId.equals(currentSchoolId)
                || !examMemberRepository.existsByUserIdAndRoleAndSchoolId(currentUserId, ExamMemberRole.AUTHOR, schoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void validateSections(CreateExamBlueprintVersionCommand command) {
        if (command.sections().isEmpty()) {
            throw new IllegalStateException("Blueprint version phải có ít nhất một section");
        }
        var sectionOrders = new HashSet<Integer>();
        for (var section : command.sections()) {
            if (!sectionOrders.add(section.order())) {
                throw new IllegalStateException("Thứ tự section không được trùng lặp");
            }
            if (slotsOf(section).isEmpty()) {
                throw new IllegalStateException("Mỗi section phải có ít nhất một slot");
            }
            var slotOrders = new HashSet<Integer>();
            for (var slot : slotsOf(section)) {
                if (!slotOrders.add(slot.order())) {
                    throw new IllegalStateException("Thứ tự slot trong section không được trùng lặp");
                }
                validateSlot(slot);
            }
        }
    }

    private void validateSlot(CreateExamBlueprintSlotCommand slot) {
        var slotType = ExamBlueprintSlotType.valueOf(slot.slotType());
        if (slotType == ExamBlueprintSlotType.FIXED) {
            if (slot.fixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (slot.selectionSpec() != null) {
                throw new IllegalStateException("Slot FIXED không được có selectionSpec");
            }
            if (!questionRepository.existsById(slot.fixedQuestionId())) {
                throw new NotFoundException("Không tìm thấy câu hỏi fixed cho slot");
            }
            return;
        }
        if (slot.fixedQuestionId() != null) {
            throw new IllegalStateException("Slot SELECTION không được có fixedQuestionId");
        }
        if (slot.selectionSpec() == null) {
            throw new IllegalStateException("Slot SELECTION bắt buộc phải có selectionSpec");
        }
    }

    private java.util.List<CreateExamBlueprintSlotCommand> slotsOf(CreateExamBlueprintSectionCommand section) {
        return section.slots() == null ? java.util.List.of() : section.slots();
    }

    private BigDecimal defaultWeight(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private OffsetDateTime effectiveFromOf(String value, OffsetDateTime defaultValue) {
        return value == null ? defaultValue : OffsetDateTime.parse(value);
    }

    private OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
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
