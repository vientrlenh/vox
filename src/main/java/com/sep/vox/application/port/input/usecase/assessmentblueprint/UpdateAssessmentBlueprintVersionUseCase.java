package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.question.Question;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
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
public class UpdateAssessmentBlueprintVersionUseCase implements IUseCase<UpdateAssessmentBlueprintVersionCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(UpdateAssessmentBlueprintVersionCommand input) {
        UpdateAssessmentBlueprintVersionCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElseThrow(() -> new ForbiddenException("Quyền truy cập bị từ chối"));

        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(command.versionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy version blueprint"));
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(version.getBlueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy blueprint đề thi"));
        if (!assessmentBlueprintRepository.canEditBlueprint(blueprint.getId(), currentUserId, currentSchoolId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ được cập nhật version ở trạng thái DRAFT");
        }
        validateSections(command);

        if (command.description() != null) {
            version.setDescription(command.description());
        }
        version.setEffectiveFrom(command.effectiveFrom() == null ? version.getEffectiveFrom() : command.effectiveFrom());
        version.setEffectiveTo(command.effectiveTo());
        AssessmentBlueprintVersion savedVersion = assessmentBlueprintVersionRepository.save(version);

        List<AssessmentBlueprintSection> existingSections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(savedVersion.getId());
        Map<UUID, AssessmentBlueprintSection> existingSectionsById = new HashMap<UUID, AssessmentBlueprintSection>();
        Map<UUID, Map<UUID, AssessmentBlueprintSlot>> existingSlotsBySectionId = new HashMap<UUID, Map<UUID, AssessmentBlueprintSlot>>();
        for (AssessmentBlueprintSection section : existingSections) {
            existingSectionsById.put(section.getId(), section);
            Map<UUID, AssessmentBlueprintSlot> slotsById = new HashMap<UUID, AssessmentBlueprintSlot>();
            for (AssessmentBlueprintSlot slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
                slotsById.put(slot.getId(), slot);
            }
            existingSlotsBySectionId.put(section.getId(), slotsById);
        }

        Set<UUID> keptSectionIds = new HashSet<UUID>();
        Set<UUID> keptSlotIds = new HashSet<UUID>();

        for (CreateAssessmentBlueprintSectionCommand sectionCommand : command.sections()) {
            AssessmentBlueprintSection currentSection = toSection(savedVersion.getId(), sectionCommand, existingSectionsById);
            AssessmentBlueprintSection savedSection = assessmentBlueprintSectionRepository.save(currentSection);
            keptSectionIds.add(savedSection.getId());

            Map<UUID, AssessmentBlueprintSlot> existingSlots = existingSlotsBySectionId.getOrDefault(savedSection.getId(), Map.of());
            for (CreateAssessmentBlueprintSlotCommand slotCommand : slotsOf(sectionCommand)) {
                AssessmentBlueprintSlot currentSlot = toSlot(savedVersion.getId(), savedSection.getId(), slotCommand, existingSlots);
                AssessmentBlueprintSlot savedSlot = assessmentBlueprintSlotRepository.save(currentSlot);
                keptSlotIds.add(savedSlot.getId());
            }
        }

        deleteRemovedSlots(existingSlotsBySectionId, keptSlotIds);
        deleteRemovedSections(existingSections, keptSectionIds);

        AssessmentBlueprintVersion recalculatedVersion = recalculateBlueprintVersionTimeLimitService.recalculate(savedVersion.getId());
        assessmentTimeQuotaGuardService.requireWithinPlan(
            blueprint.getSchoolId(),
            recalculatedVersion.getTotalTimeLimitSeconds(),
            "Phiên bản blueprint " + recalculatedVersion.getCode()
        );
        return AssessmentBlueprintVersionDtoMapper.toDto(recalculatedVersion);
    }

    private UpdateAssessmentBlueprintVersionCommand normalize(UpdateAssessmentBlueprintVersionCommand input) {
        return new UpdateAssessmentBlueprintVersionCommand(
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

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private void validateSections(UpdateAssessmentBlueprintVersionCommand command) {
        if (command.sections().isEmpty()) {
            throw new IllegalArgumentException("Blueprint version phải có ít nhất một section");
        }
        Set<Integer> sectionOrders = new HashSet<Integer>();
        BigDecimal sectionWeightSum = BigDecimal.ZERO;
        for (CreateAssessmentBlueprintSectionCommand section : command.sections()) {
            if (!sectionOrders.add(section.order())) {
                throw new IllegalArgumentException("Thứ tự section không được trùng lặp");
            }
            if (slotsOf(section).isEmpty()) {
                throw new IllegalArgumentException("Mỗi section phải có ít nhất một slot");
            }
            Set<Integer> slotOrders = new HashSet<Integer>();
            BigDecimal slotWeightSum = BigDecimal.ZERO;
            for (CreateAssessmentBlueprintSlotCommand slot : slotsOf(section)) {
                if (!slotOrders.add(slot.order())) {
                    throw new IllegalArgumentException("Thứ tự slot trong section không được trùng lặp");
                }
                validateSlot(slot);
                slotWeightSum = slotWeightSum.add(defaultWeight(slot.weight()));
            }
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalArgumentException(
                    "Tổng trọng số ô câu hỏi trong phần \"" + section.title() + "\" phải bằng 1.00");
            }
            sectionWeightSum = sectionWeightSum.add(defaultWeight(section.sectionWeight()));
        }
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException("Tổng trọng số section phải bằng 1.00");
        }
    }

    private void validateSlot(CreateAssessmentBlueprintSlotCommand slot) {
        AssessmentBlueprintSlotType slotType = slot.slotType();
        if (slotType == AssessmentBlueprintSlotType.FIXED) {
            if (slot.fixedQuestionId() == null) {
                throw new IllegalArgumentException("Slot FIXED bắt buộc phải có fixedQuestionId");
            }
            if (slot.selectionSpec() != null) {
                throw new IllegalArgumentException("Slot FIXED không được có selectionSpec");
            }
            Question question = questionRepository.findById(slot.fixedQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi fixed cho slot"));
            if (question.getStatus() != QuestionStatus.PUBLISHED) {
                throw new IllegalArgumentException("Câu hỏi fixed cho slot phải ở trạng thái PUBLISHED");
            }
            return;
        }
        if (slot.fixedQuestionId() != null) {
            throw new IllegalArgumentException("Slot SELECTION không được có fixedQuestionId");
        }
        if (slot.selectionSpec() == null) {
            throw new IllegalArgumentException("Slot SELECTION bắt buộc phải có selectionSpec");
        }
    }

    private AssessmentBlueprintSection toSection(
            UUID versionId,
            CreateAssessmentBlueprintSectionCommand command,
            Map<UUID, AssessmentBlueprintSection> existingSectionsById) {
        if (command.id() == null) {
            return AssessmentBlueprintSection.create(
                versionId,
                command.order(),
                command.title(),
                command.instruction(),
                command.sectionTimeLimitSeconds(),
                defaultWeight(command.sectionWeight()));
        }
        AssessmentBlueprintSection existing = existingSectionsById.get(command.id());
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy section cần cập nhật");
        }
        return existing.revisedWith(
            command.order(),
            command.title(),
            command.instruction(),
            command.sectionTimeLimitSeconds(),
            defaultWeight(command.sectionWeight()));
    }

    private AssessmentBlueprintSlot toSlot(
            UUID versionId,
            UUID sectionId,
            CreateAssessmentBlueprintSlotCommand command,
            Map<UUID, AssessmentBlueprintSlot> existingSlots) {
        if (command.id() == null) {
            return AssessmentBlueprintSlot.create(
                sectionId,
                versionId,
                command.order(),
                defaultWeight(command.weight()),
                command.prepTimeSecondsOverride(),
                command.responseTimeSecondsOverride(),
                command.slotType(),
                command.fixedQuestionId(),
                selectionSpecOf(command.selectionSpec()));
        }
        AssessmentBlueprintSlot existing = existingSlots.get(command.id());
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy slot cần cập nhật");
        }
        if (!existing.getSectionId().equals(sectionId)) {
            throw new IllegalArgumentException("Không được di chuyển slot sang section khác bằng PUT hiện tại");
        }
        return existing.revisedWith(
            command.order(),
            defaultWeight(command.weight()),
            command.prepTimeSecondsOverride(),
            command.responseTimeSecondsOverride(),
            command.slotType(),
            command.fixedQuestionId(),
            selectionSpecOf(command.selectionSpec()));
    }

    private void deleteRemovedSlots(Map<UUID, Map<UUID, AssessmentBlueprintSlot>> existingSlotsBySectionId, Set<UUID> keptSlotIds) {
        for (Map<UUID, AssessmentBlueprintSlot> slotsById : existingSlotsBySectionId.values()) {
            for (AssessmentBlueprintSlot slot : slotsById.values()) {
                if (!keptSlotIds.contains(slot.getId())) {
                    assessmentBlueprintSlotRepository.deleteById(slot.getId());
                }
            }
        }
    }

    private void deleteRemovedSections(java.util.List<AssessmentBlueprintSection> existingSections, Set<UUID> keptSectionIds) {
        for (AssessmentBlueprintSection section : existingSections) {
            if (!keptSectionIds.contains(section.getId())) {
                assessmentBlueprintSectionRepository.deleteById(section.getId());
            }
        }
    }

    private java.util.List<CreateAssessmentBlueprintSlotCommand> slotsOf(CreateAssessmentBlueprintSectionCommand section) {
        return section.slots() == null ? java.util.List.of() : section.slots();
    }

    private BigDecimal defaultWeight(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
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
