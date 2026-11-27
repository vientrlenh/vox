package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlot;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperItemCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.service.RecalculateAssessmentTimeDurationService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperItemDto;
import com.sep.vox.domain.mapper.AssessmentPaperItemDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.PaperTimeCalculator;
import com.sep.vox.domain.valueobject.QuestionSelectionSpec;

@Service
@RequiredArgsConstructor
public class UpdateAssessmentPaperItemUseCase implements IUseCase<UpdateAssessmentPaperItemCommand, AssessmentPaperItemDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final RecalculateAssessmentTimeDurationService recalculateAssessmentTimeDurationService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentPaperItemDto execute(UpdateAssessmentPaperItemCommand input) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentPaperItem item = findItemOfPaper(input);
        AssessmentPaper paper = assessmentPaperRepository.findById(item.getPaperId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment paper not found"));
        Assessment assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        requirePaperEditable(assessment, paper);
        assessmentPaperAuthoringAccessService.requireCanAuthor(assessment, currentUserId);

        AssessmentBlueprintSlot slot = findAssignableBlueprintSlot(item);
        Question question = findAssignableQuestion(input, assessment, slot, currentUserId);
        requireCanAssignQuestion(question, currentUserId);

        requireDurationWithinPlan(assessment, paper, item, question);
        AssessmentPaperItem savedItem = assignQuestion(item, paper, assessment, question, currentUserId);
        recalculateAssessmentTimeDurationService.recalculate(paper.getAssessmentId());
        return AssessmentPaperItemDtoMapper.toDto(savedItem);
    }

    private AssessmentPaperItem findItemOfPaper(UpdateAssessmentPaperItemCommand input) {
        AssessmentPaperItem item = assessmentPaperItemRepository.findById(input.itemId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found in the assessment paper"));
        if (!item.getPaperId().equals(input.paperId())) {
            throw new ResourceNotFoundException("Question not found in the assessment paper");
        }
        return item;
    }

    private void requirePaperEditable(Assessment assessment, AssessmentPaper paper) {
        // A class test locks its paper with a single DRAFT -> LOCKED step and reopens it freely, so LOCKED there is
        // a normal working state; only in a centralized assessment does it mean "finalized, no more edits".
        if (assessment.getKind() == AssessmentKind.CENTRALIZED && paper.getStatus() == AssessmentPaperStatus.LOCKED) {
            throw new IllegalArgumentException("The paper is locked, its questions cannot be edited");
        }
        if (assessment.getStatus() == AssessmentStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Questions cannot be edited while the assessment is in progress");
        }
    }

    private AssessmentBlueprintSlot findAssignableBlueprintSlot(AssessmentPaperItem item) {
        // H.7: only a FIXED slot has to match the blueprint exactly - changing its question must go through a version change.
        // A SELECTION slot only describes criteria in the blueprint; the paper author picks the concrete question,
        // so this is the one place where a question is assigned to that slot.
        //
        // A deleted slot (a DRAFT version can still delete slots) is treated as a free slot: the blueprint
        // constraint no longer exists, and failing closed here would only leave the paper variant stuck forever.
        AssessmentBlueprintSlot slot = item.getBlueprintSlotId() == null
            ? null
            : assessmentBlueprintSlotRepository.findById(item.getBlueprintSlotId()).orElse(null);
        if (slot != null && slot.getSlotType() == AssessmentBlueprintSlotType.FIXED) {
            throw new IllegalArgumentException(
                "This question slot is fixed by the blueprint, its question cannot be changed directly: change the blueprint version instead");
        }
        return slot;
    }

    private Question findAssignableQuestion(
            UpdateAssessmentPaperItemCommand input,
            Assessment assessment,
            AssessmentBlueprintSlot slot,
            UUID currentUserId) {
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        Question question = questionRepository.findAccessibleById(input.questionId(), currentUserId, currentSchoolId, false, false)
            .orElseThrow(() -> new ForbiddenException("You have no permission to use this question"));
        if ((assessment.getKind() == AssessmentKind.CENTRALIZED || slot != null)
                && question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only a PUBLISHED question can be assigned to a paper");
        }
        if (slot != null && slot.getSlotType() == AssessmentBlueprintSlotType.SELECTION) {
            requireMatchesSelectionSpec(slot.getSelectionSpec(), question);
        }
        return question;
    }

    private void requireCanAssignQuestion(Question question, UUID currentUserId) {
        boolean isOwner = question.isCreatedBy(currentUserId);
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (!isOwner && !isSchoolShared) {
            Optional<QuestionCollaborator> collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
            if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                throw new ForbiddenException("The READ_ONLY permission does not allow assigning questions to a paper");
            }
        }
    }

    private void requireDurationWithinPlan(Assessment assessment, AssessmentPaper paper, AssessmentPaperItem item, Question question) {
        int candidateDurationSeconds = calculatePaperDurationAfterQuestionChange(paper.getId(), item.getId(), question);
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            candidateDurationSeconds,
            "Paper " + paper.getCode()
        );
    }

    private AssessmentPaperItem assignQuestion(
            AssessmentPaperItem item,
            AssessmentPaper paper,
            Assessment assessment,
            Question question,
            UUID currentUserId) {
        item.setQuestionId(question.getId());
        AssessmentPaperItem savedItem = assessmentPaperItemRepository.save(item);

        assessmentQuestionSecureLockService.lockQuestionForAssessment(
            question.getId(),
            paper.getAssessmentId(),
            releaseModeFor(assessment),
            currentUserId
        );

        if (paper.getStatus() == AssessmentPaperStatus.APPROVED) {
            paper.setStatus(AssessmentPaperStatus.IN_REVIEW);
        }
        assessmentPaperRepository.save(paper);
        return savedItem;
    }

    /**
     * Questions of a class test unlock themselves when the test closes; for a centralized assessment the manager unlocks them.
     */
    private AssessmentSecurePoolReleaseMode releaseModeFor(Assessment assessment) {
        return assessment.getKind() == AssessmentKind.CLASS_TEST
            ? AssessmentSecurePoolReleaseMode.AUTO_AFTER_CLOSE
            : AssessmentSecurePoolReleaseMode.MANUAL;
    }

    /**
     * A SELECTION slot only describes criteria, so the assigned question must match them. The client-side picker
     * already filters, but the REST endpoint can be called directly so it must still be blocked here.
     *
     * <p>Only {@code questionType} and {@code topicId} can be checked: {@link Question}
     * has no difficulty/skillCode/targetBandLevel, so the other 3 criteria are only informational
     * for the person authoring the paper.
     */
    private void requireMatchesSelectionSpec(QuestionSelectionSpec spec, Question question) {
        if (spec == null) {
            return;
        }
        if (spec.questionType() != null && question.getType() != spec.questionType()) {
            throw new IllegalArgumentException(
                "This question slot requires a question of type " + spec.questionType() + ", the selected question does not match the criteria");
        }
        if (spec.topicId() != null && !spec.topicId().equals(question.getQuestionTopicId())) {
            throw new IllegalArgumentException("The selected question does not belong to the topic required by the question slot");
        }
    }

    private int calculatePaperDurationAfterQuestionChange(
            UUID paperId,
            UUID itemId,
            Question replacementQuestion) {
        // Projects the duration of the paper variant AFTER the question swap, BEFORE writing -- so
        // RecalculateAssessmentTimeDurationService cannot be reused (it reads from the DB, and the change is not there yet).
        List<Question> questions = new ArrayList<Question>();
        for (AssessmentPaperItem candidateItem : assessmentPaperItemRepository.findByPaperId(paperId)) {
            if (candidateItem.getId().equals(itemId)) {
                questions.add(replacementQuestion);
                continue;
            }
            if (candidateItem.getQuestionId() == null) {
                continue;
            }
            questionRepository.findById(candidateItem.getQuestionId()).ifPresent(questions::add);
        }
        // totalSeconds (including AUDIO/VIDEO playback time) because this is the measure of LENGTH against the plan --
        // see PaperTimeCalculator for why the COST estimate uses a different number.
        Map<UUID, QuestionAsset> assetByQuestionId = PaperTimeCalculator.indexByQuestionId(questionAssetRepository
            .findByQuestionIdIn(questions.stream().map(q -> q.getId()).distinct().toList()));
        return PaperTimeCalculator.breakdownOf(questions, assetByQuestionId).totalSeconds();
    }
}
