package com.sep.vox.application.port.input.usecase.assessmentpaper;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentpaper.UpdateAssessmentPaperStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.AssessmentPaperAuthoringAccessService.PaperActor;
import com.sep.vox.application.port.input.service.AssessmentPaperAutoAssigner;
import com.sep.vox.application.port.input.service.AssessmentTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.mapper.AssessmentPaperDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * The lifecycle of a paper. A centralized assessment has two paths, chosen by <b>who authored the paper</b>:
 *
 * <ul>
 *   <li><b>There is a second person</b> -- the paper was authored by someone else: DRAFT → IN_REVIEW → APPROVED → LOCKED,
 *       with {@code requireNotAuthor} blocking the author from approving their own paper. This way the
 *       {@code APPROVED} status always means "a second pair of eyes has seen it".</li>
 *   <li><b>There is no second person</b> -- the CHAIR or the school admin authors the paper: shortcut
 *       one step, DRAFT → LOCKED. Forcing them to submit for review and then approve their own paper only creates a fake
 *       {@code APPROVED} mark, while forbidding it outright means a small school cannot lock a paper to assign papers.</li>
 * </ul>
 *
 * <p>A class test always takes the shortcut: the teacher who creates the test is both the CHAIR and the author of every paper.
 */
@Service
@RequiredArgsConstructor
public class UpdateAssessmentPaperStatusUseCase implements IUseCase<UpdateAssessmentPaperStatusCommand, AssessmentPaperDto> {

    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentPaperAuthoringAccessService assessmentPaperAuthoringAccessService;
    private final AssessmentTimeQuotaGuardService assessmentTimeQuotaGuardService;
    private final AssessmentPaperAutoAssigner assessmentPaperAutoAssigner;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public AssessmentPaperDto execute(UpdateAssessmentPaperStatusCommand input) {
        UpdateAssessmentPaperStatusCommand command = normalize(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        AssessmentPaper paper = assessmentPaperRepository.findById(command.paperId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment paper not found"));
        Assessment assessment = assessmentRepository.findById(paper.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));
        PaperActor actor = assessmentPaperAuthoringAccessService.resolve(assessment, currentUserId);

        applyAction(command, paper, assessment, actor, currentUserId);

        Instant now = Instant.now();
        AssessmentPaper saved = assessmentPaperRepository.save(paper);

        // Backfill AFTER saving: AssessmentPaperAutoAssigner re-reads the paper variant statuses to decide "every paper variant is
        // LOCKED", so the new status has to be in the DB first.
        if ("LOCK".equals(command.action())) {
            assessmentPaperAutoAssigner.backfillAssessment(assessment, now, currentUserId);
        }
        return AssessmentPaperDtoMapper.toDto(saved);
    }

    private UpdateAssessmentPaperStatusCommand normalize(UpdateAssessmentPaperStatusCommand input) {
        return new UpdateAssessmentPaperStatusCommand(
            input.paperId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
    }

    private void applyAction(
            UpdateAssessmentPaperStatusCommand command,
            AssessmentPaper paper,
            Assessment assessment,
            PaperActor actor,
            UUID currentUserId) {
        switch (command.action()) {
            case "SUBMIT" -> submit(paper, assessment, actor);
            case "APPROVE" -> approve(paper, assessment, actor, currentUserId);
            case "REQUEST_REVISION" -> requestRevision(command, paper, actor, currentUserId);
            case "LOCK" -> lock(paper, assessment, actor, currentUserId);
            case "REOPEN" -> reopen(paper, actor, currentUserId);
            default -> throw new IllegalArgumentException("Invalid action");
        }
    }

    private void submit(AssessmentPaper paper, Assessment assessment, PaperActor actor) {
        requireCanAuthor(actor);
        requireAllSlotsAssigned(paper, "The paper has unassigned question slots, so it cannot be submitted for review");
        requirePaperWithinPlan(assessment, paper);
        requireTransition(paper, AssessmentPaperStatus.DRAFT, AssessmentPaperStatus.IN_REVIEW);
    }

    private void approve(AssessmentPaper paper, Assessment assessment, PaperActor actor, UUID currentUserId) {
        requireCanReview(actor);
        requireNotAuthor(paper, currentUserId);
        requirePaperWithinPlan(assessment, paper);
        requireTransition(paper, AssessmentPaperStatus.IN_REVIEW, AssessmentPaperStatus.APPROVED);
    }

    private void requestRevision(UpdateAssessmentPaperStatusCommand command, AssessmentPaper paper, PaperActor actor, UUID currentUserId) {
        requireCanReview(actor);
        requireNotAuthor(paper, currentUserId);
        if (command.note() == null || command.note().isBlank()) {
            throw new IllegalArgumentException("A change request must include feedback");
        }
        requireTransition(paper, AssessmentPaperStatus.IN_REVIEW, AssessmentPaperStatus.DRAFT);
    }

    private void lock(AssessmentPaper paper, Assessment assessment, PaperActor actor, UUID currentUserId) {
        requireCanDecide(actor);
        requirePaperWithinPlan(assessment, paper);
        if (isOneStepLock(assessment, paper, currentUserId)) {
            requireAllSlotsAssigned(paper, "The paper has unassigned question slots, so it cannot be locked");
            requireTransition(paper, AssessmentPaperStatus.DRAFT, AssessmentPaperStatus.LOCKED);
        } else {
            requireNotAuthor(paper, currentUserId);
            requireTransition(paper, AssessmentPaperStatus.APPROVED, AssessmentPaperStatus.LOCKED);
        }
    }

    private void reopen(AssessmentPaper paper, PaperActor actor, UUID currentUserId) {
        // No requireNotAuthor here: someone who just took the DRAFT -> LOCKED shortcut must be able to reopen
        // their own paper variant, otherwise they lock themselves out and the paper variant is stuck forever.
        requireCanDecide(actor);
        requireTransition(paper, AssessmentPaperStatus.LOCKED, AssessmentPaperStatus.DRAFT);
        unassignCandidatesOf(paper, currentUserId);
    }

    /**
     * Unlocking is for editing the paper variant again, and a candidate pointing at it would have the attempt changed under their feet. Return
     * them to "no paper assigned" -- the same handling as {@code DeleteAssessmentPaperUseCase}. Once it is locked again,
     * {@link AssessmentPaperAutoAssigner#backfillAssessment} backfills, so nobody is stuck forever.
     */
    private void unassignCandidatesOf(AssessmentPaper paper, UUID currentUserId) {
        List<AssessmentCandidate> affected = assessmentCandidateRepository.findByAssignedPaperId(paper.getId());
        if (affected.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        affected.forEach(candidate -> candidate.unassignPaper(now, currentUserId));
        assessmentCandidateRepository.saveAll(affected);
    }

    /**
     * Shortcut when there is no second person to approve: a class test (only one CHAIR), or a centralized
     * assessment where the decision maker is the person who authored this paper.
     */
    private boolean isOneStepLock(Assessment assessment, AssessmentPaper paper, UUID currentUserId) {
        return assessment.getKind() == AssessmentKind.CLASS_TEST || paper.isCreatedBy(currentUserId);
    }

    private void requireCanAuthor(PaperActor actor) {
        if (!actor.canAuthor()) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void requireCanReview(PaperActor actor) {
        if (!actor.canReview()) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void requireCanDecide(PaperActor actor) {
        if (!actor.canDecide()) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void requireNotAuthor(AssessmentPaper paper, UUID currentUserId) {
        if (paper.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("You cannot approve a paper you created yourself");
        }
    }

    private void requireAllSlotsAssigned(AssessmentPaper paper, String message) {
        if (assessmentPaperItemRepository.existsUnassignedItemByPaperId(paper.getId())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requirePaperWithinPlan(Assessment assessment, AssessmentPaper paper) {
        assessmentTimeQuotaGuardService.requireWithinPlan(
            assessment.getSchoolId(),
            paper.getTimeDurationSeconds(),
            "Paper " + paper.getCode()
        );
    }

    private void requireTransition(AssessmentPaper paper, AssessmentPaperStatus from, AssessmentPaperStatus to) {
        if (paper.getStatus() != from) {
            throw new IllegalArgumentException("The current paper status is not valid for this action");
        }
        paper.setStatus(to);
    }
}
