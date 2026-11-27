package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
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
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.UpdateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.service.AssessmentBlueprintStructureValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentBlueprintVersionUseCase implements IUseCase<UpdateAssessmentBlueprintVersionCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;
    private final AssessmentBlueprintStructureValidator assessmentBlueprintStructureValidator;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(UpdateAssessmentBlueprintVersionCommand input) {
        UpdateAssessmentBlueprintVersionCommand command = normalize(input);
        AssessmentBlueprintEditAccessService.EditableVersion editable =
            assessmentBlueprintEditAccessService.requireEditableVersion(command.versionId());
        AssessmentBlueprint blueprint = editable.blueprint();
        AssessmentBlueprintVersion version = editable.version();
        if (version.getStatus() != AssessmentBlueprintVersionStatus.DRAFT) {
            throw new IllegalArgumentException("A version can only be updated while it is DRAFT");
        }
        assessmentBlueprintStructureValidator.validateSections(command.sections());

        AssessmentBlueprintVersion savedVersion = assessmentBlueprintVersionRepository.save(versionWithChanges(version, command));
        replaceSectionsAndSlots(savedVersion, command);

        AssessmentBlueprintVersion recalculatedVersion = recalculateBlueprintVersionTimeLimitService.recalculate(savedVersion.getId());
        assessmentTimeQuotaGuardService.requireWithinPlan(
            blueprint.getSchoolId(),
            recalculatedVersion.getTotalTimeLimitSeconds(),
            "Blueprint version " + recalculatedVersion.getCode()
        );
        return AssessmentBlueprintVersionDtoMapper.toDto(recalculatedVersion);
    }

    private AssessmentBlueprintVersion versionWithChanges(AssessmentBlueprintVersion version, UpdateAssessmentBlueprintVersionCommand command) {
        if (command.description() != null) {
            version.setDescription(command.description());
        }
        version.setEffectiveFrom(command.effectiveFrom() == null ? version.getEffectiveFrom() : command.effectiveFrom());
        version.setEffectiveTo(command.effectiveTo());
        return version;
    }

    private void replaceSectionsAndSlots(AssessmentBlueprintVersion savedVersion, UpdateAssessmentBlueprintVersionCommand command) {
        List<AssessmentBlueprintSection> existingSections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(savedVersion.getId());
        Map<UUID, AssessmentBlueprintSection> existingSectionsById = new HashMap<UUID, AssessmentBlueprintSection>();
        for (AssessmentBlueprintSection section : existingSections) {
            existingSectionsById.put(section.getId(), section);
        }
        Map<UUID, Map<UUID, AssessmentBlueprintSlot>> existingSlotsBySectionId = existingSlotsBySectionId(existingSections);
        Set<UUID> keptSectionIds = new HashSet<UUID>();
        Set<UUID> keptSlotIds = new HashSet<UUID>();

        for (CreateAssessmentBlueprintSectionCommand sectionCommand : command.sections()) {
            AssessmentBlueprintSection currentSection = toSection(savedVersion.getId(), sectionCommand, existingSectionsById);
            AssessmentBlueprintSection savedSection = assessmentBlueprintSectionRepository.save(currentSection);
            keptSectionIds.add(savedSection.getId());
            Map<UUID, AssessmentBlueprintSlot> existingSlots = existingSlotsBySectionId.getOrDefault(savedSection.getId(), Map.of());
            saveSlots(savedVersion, savedSection, sectionCommand, existingSlots, keptSlotIds);
        }

        deleteRemovedSlots(existingSlotsBySectionId, keptSlotIds);
        deleteRemovedSections(existingSections, keptSectionIds);
    }

    private Map<UUID, Map<UUID, AssessmentBlueprintSlot>> existingSlotsBySectionId(List<AssessmentBlueprintSection> existingSections) {
        Map<UUID, Map<UUID, AssessmentBlueprintSlot>> existingSlotsBySectionId = new HashMap<UUID, Map<UUID, AssessmentBlueprintSlot>>();
        for (AssessmentBlueprintSection section : existingSections) {
            Map<UUID, AssessmentBlueprintSlot> slotsById = new HashMap<UUID, AssessmentBlueprintSlot>();
            for (AssessmentBlueprintSlot slot : assessmentBlueprintSlotRepository.findBySectionId(section.getId())) {
                slotsById.put(slot.getId(), slot);
            }
            existingSlotsBySectionId.put(section.getId(), slotsById);
        }
        return existingSlotsBySectionId;
    }

    private void saveSlots(
            AssessmentBlueprintVersion savedVersion,
            AssessmentBlueprintSection savedSection,
            CreateAssessmentBlueprintSectionCommand sectionCommand,
            Map<UUID, AssessmentBlueprintSlot> existingSlots,
            Set<UUID> keptSlotIds) {
        for (CreateAssessmentBlueprintSlotCommand slotCommand : slotsOf(sectionCommand)) {
            AssessmentBlueprintSlot currentSlot = toSlot(savedVersion.getId(), savedSection.getId(), slotCommand, existingSlots);
            AssessmentBlueprintSlot savedSlot = assessmentBlueprintSlotRepository.save(currentSlot);
            keptSlotIds.add(savedSlot.getId());
        }
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
            throw new ResourceNotFoundException("Section to update not found");
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
            return newSlot(versionId, sectionId, command);
        }
        AssessmentBlueprintSlot existing = existingSlots.get(command.id());
        if (existing == null) {
            throw new ResourceNotFoundException("Slot to update not found");
        }
        if (!existing.getSectionId().equals(sectionId)) {
            throw new IllegalArgumentException("A slot cannot be moved to another section with the current PUT");
        }
        return reviseSlot(existing, command);
    }

    private AssessmentBlueprintSlot newSlot(UUID versionId, UUID sectionId, CreateAssessmentBlueprintSlotCommand command) {
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

    private AssessmentBlueprintSlot reviseSlot(AssessmentBlueprintSlot existing, CreateAssessmentBlueprintSlotCommand command) {
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
