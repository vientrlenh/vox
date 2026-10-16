package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.command.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.assessmentsession.SubmitAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Component
public class AssessmentScheduleTimeoutGradingJob {

    private static final Logger log = LoggerFactory.getLogger(AssessmentScheduleTimeoutGradingJob.class);
    private static final Duration GRACE_PERIOD = Duration.ofMinutes(1);

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    public AssessmentScheduleTimeoutGradingJob(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase,
            SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.updateAssessmentSessionStatusUseCase = updateAssessmentSessionStatusUseCase;
        this.submitAssessmentSessionUseCase = submitAssessmentSessionUseCase;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        var threshold = Instant.now().minus(GRACE_PERIOD);
        for (var session : assessmentSessionRepository.findPastScheduleEndCandidates(threshold)) {
            try {
                var candidate = assessmentCandidateRepository.findById(session.getCandidateId()).orElse(null);
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
