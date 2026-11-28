package com.sep.vox.application.port.input.usecase.assessment;

import com.sep.vox.domain.model.assessment.AssessmentBlueprint;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.question.Question;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessment.ChangeClassTestBlueprintCommand;
import com.sep.vox.application.port.input.service.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSection;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperSection;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.QuestionRepository;

@Service
@RequiredArgsConstructor
public class ChangeClassTestBlueprintUseCase implements IUseCase<ChangeClassTestBlueprintCommand, AssessmentDto> {

    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSectionRepository assessmentBlueprintSectionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final QuestionRepository questionRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentDto execute(ChangeClassTestBlueprintCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        Assessment assessment = assessmentRepository.findById(input.assessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        requireBlueprintChangeAllowed(input, assessment, currentUserId);
        if (input.blueprintId() != null) {
            validateExistingBlueprintDuration(input, assessment);
        }

        AssessmentPaper paper = requireSinglePaper(assessment);
        clearExistingPaperContent(paper, currentUserId);

        if (input.blueprintId() != null) {
            switchToExistingBlueprint(input, assessment, paper, currentUserId);
        } else {
            // Detaching the blueprint leaves it fully empty and creates no hidden blueprint: the assessment goes back to free direct-question mode.
            assessment.setBlueprintId(null);
            assessment.setBlueprintVersionId(null);
        }

        Assessment saved = assessmentRepository.save(assessment);
        recalculateAssessmentTimeDurationService.recalculate(assessment.getId());
        return AssessmentDtoMapper.toDto(saved);
    }

    private void requireBlueprintChangeAllowed(ChangeClassTestBlueprintCommand input, Assessment assessment, UUID currentUserId) {
        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            throw new ForbiddenException("Only applies to a class test");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Access denied");
        }
        if (assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("The blueprint may only be changed before the assessment starts");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessment.getId())) {
            throw new IllegalArgumentException("The blueprint cannot be changed after a student has submitted");
        }
        if ((input.blueprintId() == null) != (input.blueprintVersionId() == null)) {
            throw new IllegalArgumentException(
                "Provide both blueprintId and blueprintVersionId, or leave both empty to detach the blueprint");
        }
    }

    /**
     * Changing the blueprint rewrites the whole content of the paper. An assessment with several papers has no
     * paper that is "the blueprint's paper", so it is rejected instead of silently overwriting the first paper.
     */
    private AssessmentPaper requireSinglePaper(Assessment assessment) {
        List<AssessmentPaper> papers = assessmentPaperRepository.findByAssessmentId(assessment.getId());
        if (papers.size() > 1) {
            throw new IllegalArgumentException(
                "The assessment has several papers: delete papers until only one is left before changing the blueprint");
        }
        return papers.stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("The assessment has no paper yet"));
    }

    private void validateExistingBlueprintDuration(ChangeClassTestBlueprintCommand input, Assessment assessment) {
        AssessmentBlueprintVersion version = requirePublishedVersionOfSchoolBlueprint(input, assessment);
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            version.getTotalTimeLimitSeconds(),
            "Blueprint version " + version.getCode()
        );
    }

    private AssessmentBlueprintVersion requirePublishedVersionOfSchoolBlueprint(
            ChangeClassTestBlueprintCommand input,
            Assessment assessment) {
        AssessmentBlueprint blueprint = assessmentBlueprintRepository.findById(input.blueprintId())
            .orElseThrow(() -> new ResourceNotFoundException("Blueprint not found"));
        if (!blueprint.getSchoolId().equals(assessment.getSchoolId())) {
            throw new IllegalArgumentException("The blueprint does not belong to the teacher's school");
        }
        AssessmentBlueprintVersion version = assessmentBlueprintVersionRepository.findById(input.blueprintVersionId())
            .orElseThrow(() -> new ResourceNotFoundException("Blueprint version not found"));
        if (!version.getBlueprintId().equals(blueprint.getId())) {
            throw new IllegalArgumentException("The version does not belong to the selected blueprint");
        }
        if (version.getStatus() != AssessmentBlueprintVersionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a PUBLISHED version may be used");
        }
        return version;
    }

    private void clearExistingPaperContent(AssessmentPaper paper, UUID currentUserId) {
        for (AssessmentPaperSection paperSection : assessmentPaperSectionRepository.findByPaperId(paper.getId())) {
            for (AssessmentPaperItem item : assessmentPaperItemRepository.findBySectionId(paperSection.getId())) {
                if (item.getQuestionId() != null) {
                    assessmentQuestionSecureLockService.unlockQuestion(item.getQuestionId(), currentUserId);
                }
                assessmentPaperItemRepository.deleteById(item.getId());
            }
            assessmentPaperSectionRepository.deleteById(paperSection.getId());
        }
    }

    private void switchToExistingBlueprint(
            ChangeClassTestBlueprintCommand input,
            Assessment assessment,
            AssessmentPaper paper,
            UUID currentUserId) {
        AssessmentBlueprintVersion version = requirePublishedVersionOfSchoolBlueprint(input, assessment);

        List<AssessmentBlueprintSection> sections = requireSortedSections(version);
        Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId = assessmentBlueprintSlotRepository.findByBlueprintVersionId(version.getId()).stream()
            .collect(Collectors.groupingBy(slot -> slot.getSectionId()));
        validateVersionWeights(sections, slotsBySectionId);
        validateReusableSlots(slotsBySectionId.values().stream().flatMap(list -> list.stream()).toList());

        assessment.setBlueprintId(input.blueprintId());
        assessment.setBlueprintVersionId(version.getId());

        for (AssessmentBlueprintSection section : sections) {
            List<AssessmentBlueprintSlot> slots = slotsBySectionId.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(slot -> slot.getOrder()))
                .toList();
            copySectionIntoPaper(section, slots, paper, assessment, currentUserId);
        }
    }

    private List<AssessmentBlueprintSection> requireSortedSections(AssessmentBlueprintVersion version) {
        List<AssessmentBlueprintSection> sections = assessmentBlueprintSectionRepository.findByBlueprintVersionId(version.getId()).stream()
            .sorted(Comparator.comparingInt(section -> section.getOrder()))
            .toList();
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("The blueprint version has no section");
        }
        return sections;
    }

    private void copySectionIntoPaper(
            AssessmentBlueprintSection section,
            List<AssessmentBlueprintSlot> slots,
            AssessmentPaper paper,
            Assessment assessment,
            UUID currentUserId) {
        AssessmentPaperSection paperSection = assessmentPaperSectionRepository.save(
            AssessmentPaperSection.fromBlueprintSection(paper.getId(), section));
        for (AssessmentBlueprintSlot slot : slots) {
            copySlotIntoPaper(slot, paperSection, paper, assessment, currentUserId);
        }
    }

    private void copySlotIntoPaper(
            AssessmentBlueprintSlot slot,
            AssessmentPaperSection paperSection,
            AssessmentPaper paper,
            Assessment assessment,
            UUID currentUserId) {
        UUID questionId = slot.getSlotType() == AssessmentBlueprintSlotType.FIXED ? slot.getFixedQuestionId() : null;
        if (questionId != null && !isFixedQuestionPublished(questionId)) {
            // The fixed question was archived after the blueprint was published: leave this slot empty, the CHAIR picks another question through the picker.
            assessmentPaperItemRepository.save(
                AssessmentPaperItem.emptySlot(paperSection.getId(), paper.getId(), slot));
            return;
        }
        AssessmentPaperItem item = AssessmentPaperItem.forBlueprintSlot(
            paperSection.getId(), paper.getId(), slot, questionId);
        assessmentPaperItemRepository.save(item);
        if (questionId != null) {
            assessmentQuestionSecureLockService.lockQuestionForAssessment(
                questionId, assessment.getId(), AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE, currentUserId
            );
        }
    }

    private boolean isFixedQuestionPublished(UUID questionId) {
        Question fixedQuestion = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Fixed question not found in the blueprint"));
        return fixedQuestion.getStatus() == QuestionStatus.PUBLISHED;
    }

    private void validateVersionWeights(List<AssessmentBlueprintSection> sections, Map<UUID, List<AssessmentBlueprintSlot>> slotsBySectionId) {
        BigDecimal sectionWeightSum = sections.stream()
            .map(section -> section.getSectionWeight() == null ? BigDecimal.ZERO : section.getSectionWeight())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        if (sectionWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException(
                "The locked blueprint version has an invalid total section weight and cannot be used for an assessment");
        }
        for (AssessmentBlueprintSection section : sections) {
            List<AssessmentBlueprintSlot> slots = slotsBySectionId.getOrDefault(section.getId(), List.of());
            BigDecimal slotWeightSum = slots.stream()
                .map(slot -> slot.getWeight() == null ? BigDecimal.ZERO : slot.getWeight())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            if (slotWeightSum.subtract(BigDecimal.ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
                throw new IllegalArgumentException(
                    "Section \"" + section.getTitle() + "\" in the blueprint has an invalid total question slot weight");
            }
        }
    }

    private void validateReusableSlots(List<AssessmentBlueprintSlot> slots) {
        for (AssessmentBlueprintSlot slot : slots) {
            if (slot.getSlotType() == AssessmentBlueprintSlotType.SELECTION) {
                continue;
            }
            if (slot.getFixedQuestionId() == null) {
                throw new IllegalArgumentException("A FIXED slot must have a fixedQuestionId");
            }
        }
    }
}
