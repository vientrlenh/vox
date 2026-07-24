package com.sep.vox.infrastructure.worker;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.ZeroScoreExamResultService;
import com.sep.vox.application.port.input.usecase.exam.ExamQuestionSecureLockService;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;

@Component
public class ExamStatusAutoTransitionJob {

    private static final Logger log = LoggerFactory.getLogger(ExamStatusAutoTransitionJob.class);

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamQuestionSecureLockService examQuestionSecureLockService;
    private final ZeroScoreExamResultService zeroScoreExamResultService;

    public ExamStatusAutoTransitionJob(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            ZeroScoreExamResultService zeroScoreExamResultService) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.zeroScoreExamResultService = zeroScoreExamResultService;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        var now = OffsetDateTime.now();

        for (var exam : examRepository.findByStatusAndOpenAtBefore(ExamStatus.SCHEDULED, now)) {
            if (exam.getKind() != ExamKind.CLASS_TEST) {
                continue;
            }
            exam.setStatus(ExamStatus.IN_PROGRESS);
            exam.setUpdatedAt(now);
            examRepository.save(exam);
            // Khớp đúng side-effect của UpdateExamStatusUseCase.START — khoá paper khi bài chính thức mở cho học sinh,
            // để không lệch hành vi giữa mở tự động (openAt) và CHAIR bấm tay.
            for (var paper : examPaperRepository.findByExamId(exam.getId())) {
                if (paper.getStatus() != ExamPaperStatus.LOCKED) {
                    paper.setStatus(ExamPaperStatus.LOCKED);
                    paper.setUpdatedAt(now);
                    examPaperRepository.save(paper);
                }
            }
            log.info("Tự động mở bài kiểm tra {} (openAt đã tới)", exam.getId());
        }

        for (var exam : examRepository.findByStatusAndCloseAtBefore(ExamStatus.IN_PROGRESS, now)) {
            if (exam.getKind() != ExamKind.CLASS_TEST) {
                continue;
            }
            exam.setStatus(ExamStatus.CLOSED);
            exam.setUpdatedAt(now);
            examRepository.save(exam);
            examQuestionSecureLockService.releaseIfAutoAfterClose(exam.getId());
            zeroScoreExamResultService.ensureZeroResultsForMissingOrEmptyAttempts(exam.getId());
            log.info("Tự động đóng bài kiểm tra {} (closeAt đã tới)", exam.getId());
        }
    }
}
