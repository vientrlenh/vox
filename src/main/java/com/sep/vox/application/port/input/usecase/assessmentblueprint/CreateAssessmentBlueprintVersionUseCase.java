package com.sep.vox.application.port.input.usecase.assessmentblueprint;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import java.math.BigDecimal;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSectionCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintSlotCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateAssessmentBlueprintVersionCommand;
import com.sep.vox.application.port.input.command.assessmentblueprint.CreateQuestionSelectionSpecCommand;
import com.sep.vox.application.port.input.service.AssessmentBlueprintEditAccessService;
import com.sep.vox.application.port.input.service.AssessmentBlueprintStructureValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateBlueprintVersionTimeLimitService;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintVersionDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class CreateAssessmentBlueprintVersionUseCase implements IUseCase<CreateAssessmentBlueprintVersionCommand, AssessmentBlueprintVersionDto> {

    private final AssessmentBlueprintEditAccessService assessmentBlueprintEditAccessService;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentBlueprintStructureValidator assessmentBlueprintStructureValidator;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateBlueprintVersionTimeLimitService recalculateBlueprintVersionTimeLimitService;

    @Override
    @Transactional
    public AssessmentBlueprintVersionDto execute(CreateAssessmentBlueprintVersionCommand input) {
        CreateAssessmentBlueprintVersionCommand command = normalize(input);
        AssessmentBlueprint blueprint = assessmentBlueprintEditAccessService.requireEditableBlueprint(command.blueprintId());
        assessmentBlueprintStructureValidator.validateSections(command.sections());

        AssessmentBlueprintVersion savedVersion = saveDraftVersion(blueprint, command);
        for (CreateAssessmentBlueprintSectionCommand sectionCommand : command.sections()) {
            saveSectionWithSlots(savedVersion, sectionCommand);
        }

        AssessmentBlueprintVersion recalculatedVersion = recalculateBlueprintVersionTimeLimitService.recalculate(savedVersion.getId());
        assessmentTimeQuotaGuardService.requireWithinPlan(
            blueprint.getSchoolId(),
            recalculatedVersion.getTotalTimeLimitSeconds(),
            "Blueprint version " + recalculatedVersion.getCode()
        );
        return AssessmentBlueprintVersionDtoMapper.toDto(recalculatedVersion);
    }

    private AssessmentBlueprintVersion saveDraftVersion(AssessmentBlueprint blueprint, CreateAssessmentBlueprintVersionCommand command) {
        Instant now = Instant.now();
        int versionNumber = assessmentBlueprintVersionRepository.nextVersionNumber(blueprint.getId());
        AssessmentBlueprintVersion version = AssessmentBlueprintVersion.draft(
            blueprint.getId(),
            versionNumber,
            blueprint.getCode() + "-V" + versionNumber,
            command.effectiveFrom() == null ? now : command.effectiveFrom(),
            command.effectiveTo());
        return assessmentBlueprintVersionRepository.save(version);
    }

    private void saveSectionWithSlots(AssessmentBlueprintVersion savedVersion, CreateAssessmentBlueprintSectionCommand sectionCommand) {
        AssessmentBlueprintSection savedSection = assessmentBlueprintSectionRepository.save(AssessmentBlueprintSection.create(
            savedVersion.getId(),
            sectionCommand.order(),
            sectionCommand.title(),
            sectionCommand.instruction(),
            sectionCommand.sectionTimeLimitSeconds(),
            defaultWeight(sectionCommand.sectionWeight())));
        for (CreateAssessmentBlueprintSlotCommand slotCommand : slotsOf(sectionCommand)) {
            assessmentBlueprintSlotRepository.save(AssessmentBlueprintSlot.create(
                savedSection.getId(),
                savedVersion.getId(),
                slotCommand.order(),
                defaultWeight(slotCommand.weight()),
                slotCommand.prepTimeSecondsOverride(),
                slotCommand.responseTimeSecondsOverride(),
                slotCommand.slotType(),
                slotCommand.fixedQuestionId(),
                selectionSpecOf(slotCommand.selectionSpec())));
        }
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
