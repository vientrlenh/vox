package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.RetryGradingAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Service
@RequiredArgsConstructor
public class RetryGradingAssessmentSessionUseCase implements IUseCase<RetryGradingAssessmentSessionCommand, UUID> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final UserContextPort userContextPort;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    @Override
    @Transactional
    public UUID execute(RetryGradingAssessmentSessionCommand input) {
        AssessmentSession session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment session not found"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Candidate of the assessment session not found"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assessment of the session not found"));

        moderationAccessService.authorize(assessment, candidate);
        requireRetryable(assessment, session, candidate);
        countSchoolRegradeAttempt(session);

        submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(session.getId()));
        return session.getId();
    }

    private void requireRetryable(Assessment assessment, AssessmentSession session, AssessmentCandidate candidate) {
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalArgumentException("The assessment scores have been published, so it cannot be re-graded");
        }
        // G.4: besides GRADING_FAILED (a technical grading error), it also allows the first AI grading for 1
        // session that was wrongly flagged as a violation (INVALID) -- already unblocked -- and has NEVER had any
        // AssessmentItemEvaluation (if there ever was one, it must be recomputed from the old data instead of calling the AI
        // again, see UnblockAssessmentCandidateUseCase).
        AssessmentCandidateResult result = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        boolean isUnblockedInvalidRetry = result != null
            && result.getStatus() == AssessmentCandidateResultStatus.INVALID
            && candidate.getBlockedAt() == null;
        if (session.getStatus() != AssessmentSessionStatus.GRADING_FAILED && !isUnblockedInvalidRetry) {
            throw new IllegalArgumentException("Only a session with a grading error can be re-graded");
        }
    }

    /**
     * The limit applies only to the SCHOOL side, and only to a real regrade (a session in grading failure).
     *
     * <p>The system administrator does not spend this limit: their run is a fix for a platform incident, and if it
     * burned the limit of the school, one mass AI failure would force hundreds of attempts to manual grading through
     * no fault of the school. The same reason will apply to the automatic re-run job later.
     *
     * <p>The unblocked INVALID branch does NOT count: that is the FIRST AI grading of the session, not a regrade
     * after a failure.
     */
    private void countSchoolRegradeAttempt(AssessmentSession session) {
        boolean schoolInitiated = !userContextPort.isSystemAdmin();
        if (schoolInitiated && session.getStatus() == AssessmentSessionStatus.GRADING_FAILED) {
            if (!session.canSchoolRequestAiRegrade()) {
                throw new IllegalArgumentException(
                    "The AI re-grading attempts for this session have been used up, please switch to manual grading"
                );
            }
            session.setSchoolRegradeCount(session.getSchoolRegradeCount() + 1);
            assessmentSessionRepository.save(session);
        }
    }
}
