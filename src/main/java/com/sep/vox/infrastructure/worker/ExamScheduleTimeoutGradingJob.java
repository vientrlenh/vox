package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.examsession.SubmitExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Component
public class ExamScheduleTimeoutGradingJob {

    private static final Logger log = LoggerFactory.getLogger(ExamScheduleTimeoutGradingJob.class);
    private static final Duration GRACE_PERIOD = Duration.ofMinutes(1);

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final SubmitExamSessionUseCase submitExamSessionUseCase;

    public ExamScheduleTimeoutGradingJob(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase,
            SubmitExamSessionUseCase submitExamSessionUseCase) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.submitExamSessionUseCase = submitExamSessionUseCase;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        var threshold = Instant.now().minus(GRACE_PERIOD);
        for (var session : examSessionRepository.findPastScheduleEndCandidates(threshold)) {
            try {
                var candidate = examCandidateRepository.findById(session.getCandidateId()).orElse(null);
                if (candidate == null || candidate.getBlockedAt() != null) {
                    continue;
                }

                if (session.getStatus() == ExamSessionStatus.IN_PROGRESS
                        || session.getStatus() == ExamSessionStatus.INTERRUPTED) {
                    updateExamSessionStatusUseCase.execute(
                        new UpdateExamSessionStatusCommand(session.getId(), ExamSessionStatus.EXPIRED));
                }

                submitExamSessionUseCase.execute(new SubmitExamSessionCommand(session.getId()));
            } catch (Exception ex) {
                log.warn("Không thể xử lý hết giờ ca thi cho phiên {}", session.getId(), ex);
            }
        }
    }
}
