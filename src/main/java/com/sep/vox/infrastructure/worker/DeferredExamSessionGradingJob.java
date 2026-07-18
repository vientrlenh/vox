package com.sep.vox.infrastructure.worker;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.command.SubmitExamSessionCommand;
import com.sep.vox.application.port.input.command.UpdateExamSessionStatusCommand;
import com.sep.vox.application.port.input.usecase.examsession.SubmitExamSessionUseCase;
import com.sep.vox.application.port.input.usecase.examsession.UpdateExamSessionStatusUseCase;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamSessionRepository;

@Component
public class DeferredExamSessionGradingJob {

    private static final Logger log = LoggerFactory.getLogger(DeferredExamSessionGradingJob.class);

    private final ExamSessionRepository examSessionRepository;
    private final UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase;
    private final SubmitExamSessionUseCase submitExamSessionUseCase;

    public DeferredExamSessionGradingJob(
            ExamSessionRepository examSessionRepository,
            UpdateExamSessionStatusUseCase updateExamSessionStatusUseCase,
            SubmitExamSessionUseCase submitExamSessionUseCase) {
        this.examSessionRepository = examSessionRepository;
        this.updateExamSessionStatusUseCase = updateExamSessionStatusUseCase;
        this.submitExamSessionUseCase = submitExamSessionUseCase;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        for (var session : examSessionRepository.findDeferredGradingCandidates(OffsetDateTime.now())) {
            try {
                var current = session;
                if (current.getStatus() == ExamSessionStatus.IN_PROGRESS || current.getStatus() == ExamSessionStatus.INTERRUPTED) {
                    updateExamSessionStatusUseCase.execute(new UpdateExamSessionStatusCommand(current.getId(), ExamSessionStatus.EXPIRED));
                    current = examSessionRepository.findById(current.getId()).orElse(current);
                }

                if (current.getStatus() == ExamSessionStatus.EXPIRED
                        || (current.getStatus() == ExamSessionStatus.SUBMITTED && current.isFlagged())) {
                    submitExamSessionUseCase.execute(new SubmitExamSessionCommand(current.getId()));
                }
            } catch (Exception ex) {
                log.warn("Không thể xử lý hoãn chấm cho phiên thi {}", session.getId(), ex);
            }
        }
    }
}
