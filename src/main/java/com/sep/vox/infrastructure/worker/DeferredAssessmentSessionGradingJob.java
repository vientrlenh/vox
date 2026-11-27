package com.sep.vox.infrastructure.worker;

import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.command.assessmentsession.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.assessmentsession.UpdateAssessmentSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.assessmentsession.SubmitAssessmentSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentsession.UpdateAssessmentSessionStatusUseCase;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeferredAssessmentSessionGradingJob {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UpdateAssessmentSessionStatusUseCase updateAssessmentSessionStatusUseCase;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    @Scheduled(fixedDelay = 60000)
    public void run() {
        for (AssessmentSession session : assessmentSessionRepository.findDeferredGradingCandidates(Instant.now())) {
            try {
                AssessmentSession current = session;
                if (current.getStatus() == AssessmentSessionStatus.IN_PROGRESS || current.getStatus() == AssessmentSessionStatus.INTERRUPTED) {
                    updateAssessmentSessionStatusUseCase.execute(new UpdateAssessmentSessionStatusCommand(current.getId(), AssessmentSessionStatus.EXPIRED));
                    current = assessmentSessionRepository.findById(current.getId()).orElse(current);
                }

                if (current.getStatus() == AssessmentSessionStatus.EXPIRED
                        || current.getStatus() == AssessmentSessionStatus.SUBMITTED) {
                    submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(current.getId()));
                }
            } catch (Exception ex) {
                log.warn("Could not process the deferred grading for session {}", session.getId(), ex);
            }
        }
    }
}
