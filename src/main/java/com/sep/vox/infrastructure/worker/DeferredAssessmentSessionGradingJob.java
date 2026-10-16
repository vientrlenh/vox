package com.sep.vox.infrastructure.worker;

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
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Component
public class DeferredAssessmentSessionGradingJob {

    private static final Logger log = LoggerFactory.getLogger(DeferredAssessmentSessionGradingJob.class);

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    public DeferredAssessmentSessionGradingJob(
            AssessmentSessionRepository assessmentSessionRepository,
            UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase,
            SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.updateAssessmentSessionStatusUseCase = updateAssessmentSessionStatusUseCase;
        this.submitAssessmentSessionUseCase = submitAssessmentSessionUseCase;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        for (var session : assessmentSessionRepository.findDeferredGradingCandidates(Instant.now())) {
            try {
                var current = session;
                if (current.getStatus() == AssessmentSessionStatus.IN_PROGRESS || current.getStatus() == AssessmentSessionStatus.INTERRUPTED) {
                    updateAssessmentSessionStatusUseCase.execute(new UpdateAssessmentSessionStatusCommand(current.getId(), AssessmentSessionStatus.EXPIRED));
                    current = assessmentSessionRepository.findById(current.getId()).orElse(current);
                }

                if (current.getStatus() == AssessmentSessionStatus.EXPIRED
                        || current.getStatus() == AssessmentSessionStatus.SUBMITTED) {
                    submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(current.getId()));
                }
            } catch (Exception ex) {
                log.warn("Không thể xử lý hoãn chấm cho phiên thi {}", session.getId(), ex);
            }
        }
    }
}
