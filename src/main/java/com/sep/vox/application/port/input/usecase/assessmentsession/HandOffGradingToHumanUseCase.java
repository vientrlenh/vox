package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.HandOffGradingToHumanCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * An attempt whose AI grading failed -> hand it to a human grader.
 *
 * <p><strong>Why a separate use case instead of reusing a grading assignment:</strong> a grading
 * assignment is attached to {@code assessment_candidate_results}, but a {@code GRADING_FAILED} session <em>has no result
 * row at all</em> -- measured 2026-08-18 on session 01a015a8: 2 answers, 0 evaluations,
 * {@code result_id} null. With no result there is nothing to assign, so the grading queue never
 * sees this attempt and it stays forever on the "Grading failed" screen.
 *
 * <p>The only job here is to <em>create the attempt to be assigned</em>: a {@code PENDING_REVIEW} result row
 * with no score. {@code GradingRoundPolicy.assignableStatuses(INITIAL)} equals exactly
 * {@code {PENDING_REVIEW}}, so right afterwards the attempt appears in the queue as UNASSIGNED
 * (the {@code unassignedOnly} predicate of JpaAssessmentGradingQueryRepository is "no assignment points
 * to this result") and the school assigns it to a teacher like any other attempt.
 *
 * <p><strong>It does not choose a teacher itself:</strong> who to assign to is a coordination decision of the
 * school -- who is free, who teaches that class, who graded this attempt in the previous round. Guessing here would hand the attempt
 * to someone who perhaps should not take it.
 *
 * <p><strong>It does NOT grade 0 and does NOT change the session status.</strong> The session stays
 * {@code GRADING_FAILED} because that is the truth: the AI's grading failed. That way the regrade-with-AI button can still be
 * used -- the two exits are not mutually exclusive, and the user can try the AI again after putting it into
 * the human grader queue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HandOffGradingToHumanUseCase implements IUseCase<HandOffGradingToHumanCommand, UUID> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;

    @Override
    @Transactional
    public UUID execute(HandOffGradingToHumanCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate of the assessment session not found"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment of the session not found"));

        moderationAccessService.authorize(assessment, candidate);
        requireHandOffable(assessment, session);

        AssessmentCandidateResult existing = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        if (existing != null) {
            logAlreadyHasResult(session, existing);
            return existing.getId();
        }

        AssessmentCandidateResult saved = assessmentCandidateResultRepository.save(
            buildPendingReviewResult(session, assessment));
        log.info(
            "Session {} (AI grading failed) moved to manual grading: created result {} in PENDING_REVIEW, "
                + "waiting for the school to assign a grader.",
            session.getId(), saved.getId());
        return saved.getId();
    }

    private void requireHandOffable(Assessment assessment, AssessmentSession session) {
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalArgumentException("The assessment scores have been published, so it cannot be moved to manual grading");
        }
        if (session.getStatus() != AssessmentSessionStatus.GRADING_FAILED) {
            throw new IllegalArgumentException("It can only be moved to manual grading when AI grading failed");
        }

        // With no answers there is nothing for the grader to look at. Assigning a blank attempt only creates a task
        // that cannot be done: the teacher opens it, sees nothing, and cannot close the assignment either because
        // there is nothing to grade.
        if (assessmentItemResponseRepository.findBySessionId(session.getId()).isEmpty()) {
            throw new IllegalArgumentException(
                "This session has no answer, so there is nothing for a grader to look at");
        }
    }

    /**
     * An existing result means the attempt is already in the grading lifecycle -- in the queue or assigned.
     * Creating another would produce a duplicate attempt; changing the status of the existing one would overwrite
     * someone else's decision.
     */
    private void logAlreadyHasResult(AssessmentSession session, AssessmentCandidateResult existing) {
        log.info(
            "Session {} already has result {} ({}), not creating it again.",
            session.getId(), existing.getId(), existing.getStatus());
    }

    /**
     * The same set of fields as persistInvalidBlockedResult (SubmitAssessmentSessionUseCase): the rubric and
     * framework must be fixed RIGHT when it is created, because the grading screen reads the scale and the list of criteria
     * from the result's own rubricVersionId. If left null the teacher opens it and has no criteria
     * to enter.
     */
    private AssessmentCandidateResult buildPendingReviewResult(AssessmentSession session, Assessment assessment) {
        AssessmentPolicy policy = assessment.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(assessment.getAssessmentPolicyId()).orElse(null);

        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setAssessmentId(session.getAssessmentId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy == null ? null : policy.getId());
        result.setPolicyVersion(policy == null ? 0 : policy.getVersion());
        result.setRubricVersionId(policy == null ? null : policy.getRubricVersionId());
        result.setFrameworkVersionId(policy == null ? null : policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy == null ? null : policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(null);
        // The score is left EMPTY, not 0. Nobody has graded yet so there is no score, and a 0 here would go straight
        // into the grade sheet as a real mark.
        result.setTotalScore(null);
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);
        return result;
    }
}
