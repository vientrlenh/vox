package com.sep.vox.application.port.input.usecase.examblueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.CreateExamBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.UpdateExamBlueprintVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintSection;
import com.sep.vox.domain.model.exam.ExamBlueprintSlot;
import com.sep.vox.domain.model.exam.ExamBlueprintSlotType;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.model.question.QuestionDifficulty;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
public class UpdateExamBlueprintVersionUseCase implements IUseCase<UpdateExamBlueprintVersionCommand, ExamBlueprintVersionDto> {

    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintRepository examBlueprintRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final ExamTimeQuotaGuardService examTimeQuotaGuardService;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    public UpdateExamBlueprintVersionUseCase(
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            QuestionRepository questionRepository,
            SchoolUserRepository schoolUserRepository,
            ExamTimeQuotaGuardService examTimeQuotaGuardService,
            RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService,
            UserContextPort userContextPort) {
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintRepository = examBlueprintRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.examTimeQuotaGuardService = examTimeQuotaGuardService;
        this.recalculateBlueprintVersionTimeLimitService = recalculateBlueprintVersionTimeLimitService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamBlueprintVersionDto execute(UpdateExamBlueprintVersionCommand input) {
        var command = normalize(input);
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        var version = examBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy version blueprint"));
        var blueprint = examBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy blueprint đề thi"));
        if (!examBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != ExamBlueprintVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ được cập nhật version ở trạng thái DRAFT");
        }
        validateSections(command);

        if (command.description() != null) {
            version.setDescription(command.description());
        }
        version.setEffectiveFrom(command.effectiveFrom() == null ? version.getEffectiveFrom() : Instant.parse(command.effectiveFrom()));
        version.setEffectiveTo(parseDateTime(command.effectiveTo()));
        version.setUpdatedAt(Instant.now());
        version.setUpdatedBy(currentUserId);
        var savedVersion = examBlueprintVersionRepository.save(version);

        var existingSections = examBlueprintSectionRepository.findByBlueprintVersionId(savedVersion.getId());
        var existingSectionsById = new HashMap<UUID, ExamBlueprintSection>();
        var existingSlotsBySectionId = new HashMap<UUID, Map<UUID, ExamBlueprintSlot>>();
        for (var section : existingSections) {
            existingSectionsById.put(section.getId(), section);
            var slotsById = new HashMap<UUID, ExamBlueprintSlot>();
            for (var slot : examBlueprintSlotRepository.findBySectionId(section.getId())) {
                slotsById.put(slot.getId(), slot);
            }
            existingSlotsBySectionId.put(section.getId(), slotsById);
        }

        var keptSectionIds = new HashSet<UUID>();
        var keptSlotIds = new HashSet<UUID>();

        for (var sectionCommand : command.sections()) {
            var currentSection = toSection(savedVersion.getId(), sectionCommand, existingSectionsById, currentUserId);
            var savedSection = examBlueprintSectionRepository.save(currentSection);
            keptSectionIds.add(savedSection.getId());

            var existingSlots = existingSlotsBySectionId.getOrDefault(savedSection.getId(), Map.of());
            for (var slotCommand : slotsOf(sectionCommand)) {
                var currentSlot = toSlot(savedVersion.getId(), savedSection.getId(), slotCommand, existingSlots, currentUserId);
                var savedSlot = examBlueprintSlotRepository.save(currentSlot);
                keptSlotIds.add(savedSlot.getId());
            }
        }

        deleteRemovedSlots(existingSlotsBySectionId, keptSlotIds);
        deleteRemovedSections(existingSections, keptSectionIds);

        var recalculatedVersion = recalculateBlueprintVersionTimeLimitService.recalculate(savedVersion.getId());
        examTimeQuotaGuardService.requireWithinPlan(
            blueprint.getSchoolId(),
            recalculatedVersion.getTotalTimeLimitSeconds(),
            "Phiên bản blueprint " + recalculatedVersion.getCode()
        );
        return ExamBlueprintVersionDtoMapper.toDto(recalculatedVersion);
    }

    private UpdateExamBlueprintVersionCommand normalize(UpdateExamBlueprintVersionCommand input) {
        return new UpdateExamBlueprintVersionCommand(
            input.versionId(),
            StringNormalization.trimAndCollapseSpaces(input.description()),
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

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validateSections(UpdateExamBlueprintVersionCommand command) {
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

    private void validateSlot(CreateExamBlueprintSlotCommand slot) {
        var slotType = ExamBlueprintSlotType.valueOf(slot.slotType());
        if (slotType == ExamBlueprintSlotType.FIXED) {
            if (slot.fixedQuestionId() == null) {
                throw new IllegalStateException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (slot.selectionSpec() != null) {
                throw new IllegalStateException("Slot FIXED không được có selectionSpec");
            }
            var question = questionRepository.findById(slot.fixedQuestionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi fixed cho slot"));
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

    private ExamBlueprintSection toSection(
            UUID versionId,
            CreateExamBlueprintSectionCommand command,
            Map<UUID, ExamBlueprintSection> existingSectionsById,
            UUID currentUserId) {
        var now = Instant.now();
        if (command.id() == null) {
            return new ExamBlueprintSection(
                versionId,
                command.order(),
                command.title(),
                command.instruction(),
                command.sectionTimeLimitSeconds(),
                defaultWeight(command.sectionWeight()),
                now,
                now,
                currentUserId,
                currentUserId
            );
        }
        var existing = existingSectionsById.get(command.id());
        if (existing == null) {
            throw new NotFoundException("Không tìm thấy section cần cập nhật");
        }
        return new ExamBlueprintSection(
            existing.getId(),
            existing.getBlueprintVersionId(),
            command.order(),
            command.title(),
            command.instruction(),
            command.sectionTimeLimitSeconds(),
            defaultWeight(command.sectionWeight()),
            existing.getCreatedAt(),
            now,
            existing.getCreatedBy(),
            currentUserId
        );
    }

    private ExamBlueprintSlot toSlot(
            UUID versionId,
            UUID sectionId,
            CreateExamBlueprintSlotCommand command,
            Map<UUID, ExamBlueprintSlot> existingSlots,
            UUID currentUserId) {
        var now = Instant.now();
        if (command.id() == null) {
            return new ExamBlueprintSlot(
                sectionId,
                versionId,
                command.order(),
                defaultWeight(command.weight()),
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
        }
        var existing = existingSlots.get(command.id());
        if (existing == null) {
            throw new NotFoundException("Không tìm thấy slot cần cập nhật");
        }
        if (!existing.getSectionId().equals(sectionId)) {
            throw new IllegalStateException("Không được di chuyển slot sang section khác bằng PUT hiện tại");
        }
        return new ExamBlueprintSlot(
            existing.getId(),
            existing.getSectionId(),
            existing.getBlueprintVersionId(),
            command.order(),
            defaultWeight(command.weight()),
            command.prepTimeSecondsOverride(),
            command.responseTimeSecondsOverride(),
            ExamBlueprintSlotType.valueOf(command.slotType()),
            command.fixedQuestionId(),
            selectionSpecOf(command.selectionSpec()),
            existing.getCreatedAt(),
            now,
            existing.getCreatedBy(),
            currentUserId
        );
    }

    private void deleteRemovedSlots(Map<UUID, Map<UUID, ExamBlueprintSlot>> existingSlotsBySectionId, Set<UUID> keptSlotIds) {
        for (var slotsById : existingSlotsBySectionId.values()) {
            for (var slot : slotsById.values()) {
                if (!keptSlotIds.contains(slot.getId())) {
                    examBlueprintSlotRepository.deleteById(slot.getId());
                }
            }
        }
    }

    private void deleteRemovedSections(java.util.List<ExamBlueprintSection> existingSections, Set<UUID> keptSectionIds) {
        for (var section : existingSections) {
            if (!keptSectionIds.contains(section.getId())) {
                examBlueprintSectionRepository.deleteById(section.getId());
            }
        }
    }

    private java.util.List<CreateExamBlueprintSlotCommand> slotsOf(CreateExamBlueprintSectionCommand section) {
        return section.slots() == null ? java.util.List.of() : section.slots();
    }

    private BigDecimal defaultWeight(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
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
