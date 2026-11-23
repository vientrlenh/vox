package com.sep.vox.infrastructure.worker;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.command.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.assessmentsession.SubmitAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentScheduleTimeoutGradingJob {

    private static final Duration GRACE_PERIOD = Duration.ofMinutes(1);

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    @Scheduled(fixedDelay = 60000)
    public void run() {
        Instant threshold = Instant.now().minus(GRACE_PERIOD);
        for (AssessmentSession session : assessmentSessionRepository.findPastScheduleEndCandidates(threshold)) {
            try {
                AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId()).orElse(null);
                if (candidate == null || candidate.getBlockedAt() != null) {
                    continue;
                }

                if (session.getStatus() == AssessmentSessionStatus.IN_PROGRESS
                        || session.getStatus() == AssessmentSessionStatus.INTERRUPTED) {
                    updateAssessmentSessionStatusUseCase.execute(
                        new UpdateAssessmentSessionStatusCommand(session.getId(), AssessmentSessionStatus.EXPIRED));
                }

                submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(session.getId()));
            } catch (Exception ex) {
                log.warn("Không thể xử lý hết giờ ca thi cho phiên {}", session.getId(), ex);
            }
        }
    }
}
