package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
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
public class CreateAssessmentBlueprintVersionUseCase implements IUseCase<CreateAssessmentBlueprintVersionCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    public CreateAssessmentBlueprintVersionUseCase(
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService,
            RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService,
            UserContextPort userContextPort) {
        this.assessmentBlueprintRepository = assessmentBlueprintRepository;
        this.assessmentBlueprintVersionRepository = assessmentBlueprintVersionRepository;
        this.assessmentBlueprintSectionRepository = assessmentBlueprintSectionRepository;
        this.assessmentBlueprintSlotRepository = assessmentBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.assessmentTimeQuotaGuardService = assessmentTimeQuotaGuardService;
        this.recalculateBlueprintVersionTimeLimitService = recalculateBlueprintVersionTimeLimitService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(CreateAssessmentBlueprintVersionCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var blueprint = assessmentBlueprintRepository.findById(command.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        validateSections(command);

        var now = Instant.now();
        var versionNumber = assessmentBlueprintVersionRepository.nextVersionNumber(blueprint.getId());
        var version = new AssessmentBlueprintVersion(
            blueprint.getId(),
            versionNumber,
            blueprint.getCode() + "-V" + versionNumber,
            null,
            AssessmentBlueprintVersionStatus.DRAFT,
            null,
            effectiveFromOf(command.effectiveFrom(), now),
            parseDateTime(command.effectiveTo()),
            now,
            now,
            currentUserId,
            currentUserId
        );
        var savedVersion = assessmentBlueprintVersionRepository.save(version);

        for (var sectionCommand : command.sections()) {
            var savedSection = assessmentBlueprintSectionRepository.save(new AssessmentBlueprintSection(
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
                assessmentBlueprintSlotRepository.save(new AssessmentBlueprintSlot(
                    savedSection.getId(),
                    savedVersion.getId(),
                    slotCommand.order(),
                    defaultWeight(slotCommand.weight()),
                    slotCommand.prepTimeSecondsOverride(),
                    slotCommand.responseTimeSecondsOverride(),
                    AssessmentBlueprintSlotType.valueOf(slotCommand.slotType()),
                    slotCommand.fixedQuestionId(),
                    selectionSpecOf(slotCommand.selectionSpec()),
                    now,
                    now,
                    currentUserId,
                    currentUserId
                ));
            }
        }

        var recalculatedVersion = recalculateBlueprintVersionTimeLimitService.recalculate(savedVersion.getId());
        assessmentTimeQuotaGuardService.requireWithinPlan(
            blueprint.getSchoolId(),
            recalculatedVersion.getTotalTimeLimitSeconds(),
            "Phiên bản blueprint " + recalculatedVersion.getCode()
        );
        return AssessmentBlueprintVersionDtoMapper.toDto(recalculatedVersion);
    }

    private CreateAssessmentBlueprintVersionCommand normalize(CreateAssessmentBlueprintVersionCommand input) {
        return new CreateAssessmentBlueprintVersionCommand(
            input.blueprintId(),
            input.totalTimeLimitSeconds(),
            input.effectiveFrom(),
            input.effectiveTo(),
            input.sections() == null ? java.util.List.of() : input.sections().stream()
                .map(this::normalizeSection)
                .toList()
        );
    }

    private CreateAssessmentBlueprintSectionCommand normalizeSection(CreateAssessmentBlueprintSectionCommand input) {
        return new CreateAssessmentBlueprintSectionCommand(
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

    private CreateAssessmentBlueprintSlotCommand normalizeSlot(CreateAssessmentBlueprintSlotCommand input) {
        return new CreateAssessmentBlueprintSlotCommand(
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

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validateSections(CreateAssessmentBlueprintVersionCommand command) {
        if (command.sections().isEmpty()) {
            throw new IllegalStateException("Blueprint version phải có ít nhất một section");
        }
        var sectionOrders = new HashSet<Integer>();
        var sectionWeightSum = BigDecimal.ZERO;
        for (var section : command.sections()) {
            if (!sectionOrders.add(section.order())) {
                throw new IllegalStateException("Thứ tự section không được trùng lặp");
            }
            if (slotsOf(section).isEmpty()) {
                throw new IllegalStateException("Mỗi section phải có ít nhất một slot");
            }
            var slotOrders = new HashSet<Integer>();
            var slotWeightSum = BigDecimal.ZERO;
            for (var slot : slotsOf(section)) {
                if (!slotOrders.add(slot.order())) {
                    throw new IllegalStateException("Thứ tự slot trong section không được trùng lặp");
                }
                validateSlot(slot);
                slotWeightSum = slotWeightSum.add(defaultWeight(slot.weight()));
            }
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalStateException(
                    "Tổng trọng số ô câu hỏi trong phần \"" + section.title() + "\" phải bằng 1.00");
            }
            sectionWeightSum = sectionWeightSum.add(defaultWeight(section.sectionWeight()));
        }
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalStateException("Tổng trọng số section phải bằng 1.00");
        }
    }

    private void validateSlot(CreateAssessmentBlueprintSlotCommand slot) {
        var slotType = AssessmentBlueprintSlotType.valueOf(slot.slotType());
        if (slotType == AssessmentBlueprintSlotType.FIXED) {
            if (slot.fixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (slot.selectionSpec() != null) {
                throw new IllegalStateException("Slot FIXED không được có selectionSpec");
            }
            var question = questionRepository.findById(slot.fixedQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi fixed cho slot"));
            if (question.getStatus() != QuestionStatus.PUBLISHED) {
                throw new IllegalStateException("Câu hỏi fixed cho slot phải ở trạng thái PUBLISHED");
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

    private java.util.List<CreateAssessmentBlueprintSlotCommand> slotsOf(CreateAssessmentBlueprintSectionCommand section) {
        return section.slots() == null ? java.util.List.of() : section.slots();
    }

    private BigDecimal defaultWeight(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private Instant effectiveFromOf(String value, Instant defaultValue) {
        return value == null ? defaultValue : Instant.parse(value);
    }

    private Instant parseDateTime(String value) {
        return value == null ? null : Instant.parse(value);
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
