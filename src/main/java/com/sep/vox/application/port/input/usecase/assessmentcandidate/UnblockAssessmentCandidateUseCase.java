package com.sep.vox.application.port.input.usecase.assessmentcandidate;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentcandidate.UnblockAssessmentCandidateCommand;
import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.UpsertAssessmentCandidateResultUseCase;
import com.sep.vox.domain.dto.AssessmentCandidateDto;
import com.sep.vox.domain.mapper.AssessmentCandidateDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnblockAssessmentCandidateUseCase implements IUseCase<UnblockAssessmentCandidateCommand, AssessmentCandidateDto> {

    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final UpsertAssessmentCandidateResultUseCase upsertAssessmentCandidateResultUseCase;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;

    @Override
    @Transactional
    public AssessmentCandidateDto execute(UnblockAssessmentCandidateCommand input) {
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(input.candidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        Assessment assessment = assessmentRepository.findById(candidate.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment of the candidate not found"));

        moderationAccessService.authorize(assessment, candidate);
        // G.4: the only hard cut-off is RESULTS_PUBLISHED -- before that a block can still be lifted,
        // even after the assessment has closed (while it is in the review phase before publishing).
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalArgumentException("The assessment results have been published, no further change is possible");
        }

        Instant now = Instant.now();
        candidate.setBlockedAt(null);
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(moderationAccessService.getCurrentUserId());
        AssessmentCandidate saved = assessmentCandidateRepository.save(candidate);

        recomputeInvalidatedSessions(candidate.getId());
        return AssessmentCandidateDtoMapper.toDto(saved);
    }

    /**
     * G.4 case 2: after a second look it is NOT a violation - recompute the score for every session of the candidate who
     * was INVALID because of a wrongly flagged violation. If an AssessmentItemEvaluation already existed
     * -> recompute from the old data (human scores take priority, the AI is not called again); if there never was one
     * -> move to PENDING_REVIEW for the teacher to grade by hand, WITHOUT calling the AI (see
     * {@link #moveToPendingReviewWithoutScores}).
     */
    private void recomputeInvalidatedSessions(java.util.UUID candidateId) {
        for (AssessmentSession session : assessmentSessionRepository.findByCandidateId(candidateId)) {
            AssessmentCandidateResult result = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
            if (result == null || result.getStatus() != AssessmentCandidateResultStatus.INVALID) {
                continue;
            }

            List<UUID> responseIds = assessmentItemResponseRepository.findBySessionId(session.getId()).stream()
                .map(response -> response.getId())
                .toList();
            boolean hasEvaluations = !assessmentItemEvaluationRepository.findByResponseIdIn(responseIds).isEmpty();
            if (hasEvaluations) {
                AssessmentCandidateResult recalculated = upsertAssessmentCandidateResultUseCase.execute(session.getId());
                // Lifting the block can pull a submission from INVALID back to PENDING_REVIEW -- a class test needs
                // a new assignment before the teacher who owns the test can grade it again.
                classTestGradingAssignmentService.ensureAssignmentForResult(recalculated);
            } else {
                moveToPendingReviewWithoutScores(result, session.getId());
            }
        }
    }

    /**
     * An attempt that has NEVER had any question graded: moves straight to {@code PENDING_REVIEW} for the teacher
     * to grade by hand, WITHOUT calling the AI.
     *
     * <p><strong>Why the AI call was dropped (2026-08-16):</strong> the previous version ran
     * {@code retryGradingAssessmentSession} in this branch. But an attempt that lands here is one that was forcibly
     * ended midway -- the candidate has only spoken one or two questions, often leaving one unfinished. Making the AI grade
     * such an attempt both wastes a call and produces a meaningless score, while what the teacher
     * needs to make a judgment is the recording of the speech, not a machine score. That recording is still intact
     * in {@code assessment_item_response_turns} and is now shown on the grading screen
     * (JpaAssessmentGradingQueryRepository#turnsForItem).
     *
     * <p><strong>Why NOT go through {@code UpsertAssessmentCandidateResultUseCase}:</strong> it calls
     * {@code AssessmentSessionResultCalculator.calculate}, which throws {@code ResourceNotFoundException}
     * as soon as an answer has no evaluation (lines 82-85). With no evaluation there is nothing
     * to compute -- setting the status directly is the right way, rather than loosening a function shared by every
     * scoring path just to serve this case.
     *
     * <p>The score stays as it is (there is no score yet to change); once the teacher finishes grading, that grading
     * produces the evaluation, and from then on every ordinary scoring path runs normally again.
     */
    private void moveToPendingReviewWithoutScores(
            AssessmentCandidateResult result, UUID sessionId) {
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);
        assessmentCandidateResultRepository.save(result);
        log.info(
            "Unblocking session {}: there is no evaluation yet, moving to PENDING_REVIEW for the teacher to grade manually (no AI call).",
            sessionId
        );
    }
}
