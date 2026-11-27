package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.AssessmentAttemptForceEndRequestedExternalEvent;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.ForceEndAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentCandidateResultFinalizationService;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForceEndAssessmentSessionUseCase implements IUseCase<ForceEndAssessmentSessionCommand, UUID> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final AssessmentCandidateResultFinalizationService assessmentCandidateResultFinalizationService;
    private final ExternalEventPublisherPort externalEventPublisherPort;

    @Override
    @Transactional
    public UUID execute(ForceEndAssessmentSessionCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate of the assessment session not found"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment of the session not found"));

        moderationAccessService.authorize(assessment, candidate);
        requireForceEndable(assessment, session);
        boolean isLiveSession = isLiveSession(session);
        boolean isAlreadyGraded = session.getStatus() == AssessmentSessionStatus.GRADED;

        Instant now = Instant.now();
        UUID currentUserId = moderationAccessService.getCurrentUserId();
        String normalizedReason = requireReason(input.reason());
        markForceEnded(session, candidate, normalizedReason, isLiveSession, now, currentUserId);

        finishForceEnd(session, isAlreadyGraded, normalizedReason, currentUserId);
        return session.getId();
    }

    private void finishForceEnd(
        AssessmentSession session, boolean isAlreadyGraded, String normalizedReason, UUID currentUserId) {
        if (isAlreadyGraded) {
            // G.4 case 1: a late violation found on an attempt that is already graded - settle INVALID/0 points
            // right away, without waiting until the assessment publishes its results.
            assessmentCandidateResultFinalizationService.invalidateNow(session.getId(), currentUserId);
            return;
        }
        publishForceEndRequested(session, normalizedReason);
    }

    private void requireForceEndable(Assessment assessment, AssessmentSession session) {
        // G.4: the only hard stop is RESULTS_PUBLISHED - before that (even after the assessment has closed) a late
        // violation can still be found by reviewing the log/video.
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalArgumentException("The assessment results have been published, no further change is possible");
        }
        boolean isAlreadyGraded = session.getStatus() == AssessmentSessionStatus.GRADED;
        if (!isLiveSession(session) && !isAlreadyGraded) {
            throw new IllegalArgumentException("Only a session that is in progress or already graded can be force-ended");
        }
    }

    private boolean isLiveSession(AssessmentSession session) {
        return session.getStatus() == AssessmentSessionStatus.IN_PROGRESS
            || session.getStatus() == AssessmentSessionStatus.INTERRUPTED;
    }

    private String requireReason(String reason) {
        String normalizedReason = StringNormalization.trimAndCollapseSpaces(reason);
        if (normalizedReason == null || normalizedReason.isEmpty()) {
            throw new IllegalArgumentException("A reason is required when force-ending a session");
        }
        return normalizedReason;
    }

    /**
     * DELIBERATELY does NOT fill in the empty answer here.
     *
     * <p>The previous version called backfill() at this point, based on a WRONG assumption: that a forced end
     * is final.
     *
     * <p>It is not final. Unblocking moves the session INTERRUPTED back to IN_PROGRESS
     * (VerifyAssessmentScheduleOtpUseCase#findResumableSession) and the candidate continues with exactly the question that was just
     * filled in -- producing a SECOND response for the same paper item. The filled-in version carries a random id while the
     * real one carries the deterministic id generated by Python, so the two rows can never be merged.
     *
     * <p>Measured 2026-08-18, session 01a012cf: a paper of 2 questions but 3 responses. Question 2 carried both an empty
     * 0-score row (filled in at 03:01:32) and the real spoken answer 8.38 (03:04:04). The score of that part was dragged down to
     * average(0, 8.38) = 4.19, the attempt total dropped from 8.06 to 5.96 and fell two grade levels --
     * the candidate lost 2.1 points. The grading screen showed 3 tabs, and because the form is keyed by paperItemId the empty
     * tab even borrowed the criterion scores of the real question.
     *
     * <p>Filling in 0 now happens at the truly final moment: when submitting (SubmitAssessmentSessionUseCase)
     * or when the teacher finalizes for publishing (UpholdResultUseCase, which checks that the session is no longer
     * RESUMABLE). The original worry -- the result page breaking for lack of an evaluation -- is gone:
     * ViewAssessmentSessionResultUseCase builds the question list straight from the responses and leaves the score null.
     */
    private void markForceEnded(
        AssessmentSession session,
        AssessmentCandidate candidate,
        String normalizedReason,
        boolean isLiveSession,
        Instant now,
        UUID currentUserId) {
        session.setFlagged(true);
        session.setFlagReason(normalizedReason);
        if (isLiveSession) {
            session.setStatus(AssessmentSessionStatus.INTERRUPTED);
        }

        candidate.setBlockedAt(now);
        candidate.setUpdatedAt(now);
        candidate.setUpdatedBy(currentUserId);

        assessmentSessionRepository.save(session);
        assessmentCandidateRepository.save(candidate);
    }

    private void publishForceEndRequested(AssessmentSession session, String normalizedReason) {
        try {
            externalEventPublisherPort.publish(
                new AssessmentAttemptForceEndRequestedExternalEvent(session.getId().toString(), normalizedReason)
            );
        } catch (RuntimeException ex) {
            log.warn("Cannot publish the force-end event for session {}", session.getId(), ex);
        }
    }
}
