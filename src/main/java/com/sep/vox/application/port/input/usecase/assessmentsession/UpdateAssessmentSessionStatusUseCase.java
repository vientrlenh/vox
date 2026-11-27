package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.UpdateAssessmentSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.assessmentsession.AssessmentSessionResponse;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AiUsageRecordRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateAssessmentSessionStatusUseCase implements IUseCase<UpdateAssessmentSessionStatusCommand, AssessmentSessionResponse> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    @Override
    @Transactional
    public AssessmentSessionResponse execute(UpdateAssessmentSessionStatusCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));

        if (isSubmittedAfterExpiry(session, input)) {
            return CreateAssessmentSessionUseCase.toResponse(session);
        }

        validateTransition(session.getStatus(), input.status());
        session.setStatus(input.status());
        if ((input.status() == AssessmentSessionStatus.SUBMITTED || input.status() == AssessmentSessionStatus.EXPIRED)
                && session.getSubmittedAt() == null) {
            session.setSubmittedAt(Instant.now());
        }
        applyGradingFailure(session, input);

        assessmentSessionRepository.save(session);
        if (input.status() == AssessmentSessionStatus.SUBMITTED && canSubmitImmediately(session)) {
            submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(session.getId()));
        }

        return CreateAssessmentSessionUseCase.toResponse(
            assessmentSessionRepository.findById(session.getId()).orElse(session)
        );
    }

    /**
     * B.4: race - the assessment-ended job has just set EXPIRED right before the client managed to send SUBMITTED.
     * Treated as a successful no-op that keeps EXPIRED (grading has happened or will happen through the EXPIRED path),
     * without throwing, so the student does not see an error right when they think they have finished submitting.
     */
    private boolean isSubmittedAfterExpiry(AssessmentSession session, UpdateAssessmentSessionStatusCommand input) {
        return session.getStatus() == AssessmentSessionStatus.EXPIRED && input.status() == AssessmentSessionStatus.SUBMITTED;
    }

    /**
     * The grading failure reason lives exactly as long as that failure.
     *
     * <p>LEAVING GRADING_FAILED means it must be cleared: the only way out is to GRADING (regrade), and
     * if that attempt succeeds while the old message stays behind, the triage page would still count this session in the
     * incident group -- and the on-call person would handle again exactly what has just been fixed. It is cleared right here, not waiting until
     * grading finishes, because between the two moments the session is GRADING so the
     * {@code chk_assessment_sessions_grading_error_only_when_failed} constraint in the DB would reject the whole record.
     *
     * <p>ENTERING GRADING_FAILED with a null {@code gradingFailure} also writes null: that is the DLT branch, and
     * "reason unknown" is the correct answer, not missing data.
     */
    private void applyGradingFailure(AssessmentSession session,
            UpdateAssessmentSessionStatusCommand input) {
        if (input.status() != AssessmentSessionStatus.GRADING_FAILED) {
            session.clearGradingFailure();
            return;
        }
        session.setGradingError(input.gradingFailure() == null ? null : input.gradingFailure().error());
        session.setGradingRetryCount(input.gradingFailure() == null ? null : input.gradingFailure().retryCount());
        waiveFailedRoundCost(session.getId());
    }

    /**
     * The grading attempt that just failed must NOT be charged to the school.
     *
     * <p>The rule is "charge for the work that PRODUCES A USABLE RESULT", not "forgive what fails". A failed
     * attempt leaves no {@code assessment_candidate_results} row, so the school received nothing --
     * very different from a speaking practice turn that has already answered the student, which is still charged whatever happens afterwards
     * (see {@code SubmitPracticeTurnUseCase}).
     *
     * <p>Waived RIGHT HERE rather than at regrading time: leaving it until then makes the money depend on whether someone
     * goes to fix it -- neglect it and it is free, fix it and you are charged for the failed attempt too. Exactly the
     * behavior we want to encourage would be the only one that is penalized.
     *
     * <p>Only touches rows that are NOT YET SETTLED. A successful grading attempt before (if any) has already stamped
     * {@code charged_at} and must be kept as it is: waiving over them would refund work that was fully delivered.
     */
    private void waiveFailedRoundCost(UUID sessionId) {
        int waived = aiUsageRecordRepository.markWaivedByAssessmentSessionId(sessionId, Instant.now());
        if (waived > 0) {
            log.info("Waived {} AI cost rows of the failed grading run in session {}", waived, sessionId);
        }
    }

    /**
     * B.0: only blockedAt decides whether to postpone or grade right away -- flagged only affects whether
     * it counts toward the official result (sections G/I), it does not postpone AI grading.
     */
    private boolean canSubmitImmediately(AssessmentSession session) {
        return assessmentCandidateRepository.findById(session.getCandidateId())
            .map(candidate -> candidate.getBlockedAt() == null)
            .orElse(false);
    }

    private void validateTransition(AssessmentSessionStatus from, AssessmentSessionStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Invalid assessment session status");
        }
        if (from == to) {
            return;
        }
        if (isAllowedTransition(from, to)) {
            return;
        }
        throw new IllegalArgumentException("Cannot change the assessment session status from " + from + " to " + to);
    }

    private boolean isAllowedTransition(AssessmentSessionStatus from, AssessmentSessionStatus to) {
        return switch (from) {
            case IN_PROGRESS -> to == AssessmentSessionStatus.SUBMITTED
                || to == AssessmentSessionStatus.INTERRUPTED
                || to == AssessmentSessionStatus.EXPIRED;
            case INTERRUPTED -> to == AssessmentSessionStatus.IN_PROGRESS || to == AssessmentSessionStatus.EXPIRED;
            case SUBMITTED -> to == AssessmentSessionStatus.GRADING;
            case EXPIRED -> to == AssessmentSessionStatus.GRADING;
            case GRADING -> to == AssessmentSessionStatus.GRADED || to == AssessmentSessionStatus.GRADING_FAILED;
            case GRADING_FAILED -> to == AssessmentSessionStatus.GRADING;
            case GRADED -> to == AssessmentSessionStatus.GRADING;
            // DELETED is a stopping point with no way forward: restoring a deleted session is a direct data
            // fix (a soft delete has no restore path through the API), not a normal status
            // transition.
            case DELETED -> false;
        };
    }
}
