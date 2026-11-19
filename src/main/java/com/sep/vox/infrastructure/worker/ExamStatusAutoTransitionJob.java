package com.sep.vox.infrastructure.worker;

import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamPaper;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.ExamHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.ExamScheduleClosureService;
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
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final ExamHumanGradingNotificationService examHumanGradingNotificationService;
    private final ExamScheduleClosureService examScheduleClosureService;

    public ExamStatusAutoTransitionJob(
            ExamRepository examRepository,
            ExamPaperRepository examPaperRepository,
            ExamQuestionSecureLockService examQuestionSecureLockService,
            ZeroScoreExamResultService zeroScoreExamResultService,
            ClassTestGradingAssignmentService classTestGradingAssignmentService,
            ExamHumanGradingNotificationService examHumanGradingNotificationService,
            ExamScheduleClosureService examScheduleClosureService) {
        this.examRepository = examRepository;
        this.examPaperRepository = examPaperRepository;
        this.examQuestionSecureLockService = examQuestionSecureLockService;
        this.zeroScoreExamResultService = zeroScoreExamResultService;
        this.classTestGradingAssignmentService = classTestGradingAssignmentService;
        this.examHumanGradingNotificationService = examHumanGradingNotificationService;
        this.examScheduleClosureService = examScheduleClosureService;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        Instant now = Instant.now();

        for (Exam exam : examRepository.findByStatusAndOpenAtBefore(ExamStatus.SCHEDULED, now)) {
            if (exam.getKind() != ExamKind.CLASS_TEST) {
                continue;
            }
            exam.setStatus(ExamStatus.IN_PROGRESS);
            exam.setUpdatedAt(now);
            examRepository.save(exam);
            // Matches exactly the side-effect of UpdateExamStatusUseCase.START — locks the paper when the official exam opens for students,
            // so behavior does not drift between the automatic opening (openAt) and the CHAIR pressing it manually.
            for (ExamPaper paper : examPaperRepository.findByExamId(exam.getId())) {
                if (paper.getStatus() != ExamPaperStatus.LOCKED) {
                    paper.setStatus(ExamPaperStatus.LOCKED);
                    paper.setUpdatedAt(now);
                    examPaperRepository.save(paper);
                }
            }
            log.info("Tự động mở bài kiểm tra {} (openAt đã tới)", exam.getId());
        }

        for (Exam exam : examRepository.findByStatusAndCloseAtBefore(ExamStatus.IN_PROGRESS, now)) {
            if (exam.getKind() != ExamKind.CLASS_TEST) {
                continue;
            }
            // Normally the guard never blocks here: closeAt is always after the endDate of every shift
            // (guaranteed by requireClassTestScheduleWindow). This branch protects against inconsistent data, and the continue
            // makes it heal itself on the next tick instead of cutting into an exam that is running.
            try {
                examScheduleClosureService.requireNoActiveSessionInOngoingSchedule(exam.getId(), now);
            } catch (IllegalStateException ex) {
                log.info("Hoãn tự đóng bài kiểm tra {}: {}", exam.getId(), ex.getMessage());
                continue;
            }
            exam.setStatus(ExamStatus.CLOSED);
            exam.setUpdatedAt(now);
            examRepository.save(exam);
            // Matches exactly the side-effect of UpdateExamStatusUseCase.CLOSE: most class tests are closed through
            // this path, and missing it here leaves the exam shift in PUBLISHED forever.
            examScheduleClosureService.closeSchedulesForExam(exam.getId(), null, now);
            examQuestionSecureLockService.releaseIfAutoAfterClose(exam.getId());
            zeroScoreExamResultService.ensureZeroResultsForMissingOrEmptyAttempts(exam.getId());
            // Most class tests are closed through this path rather than by the CHAIR manually — missing
            // it here means losing the grading assignments for almost all attempts.
            classTestGradingAssignmentService.ensureAssignmentsForExam(exam.getId());
            // Matches the side-effect of UpdateExamStatusUseCase.CLOSE: most class tests are closed through
            // this path, and missing it here means teachers are never told there is an attempt waiting for grading.
            examHumanGradingNotificationService.publishIfPendingReview(exam, now);
            log.info("Tự động đóng bài kiểm tra {} (closeAt đã tới)", exam.getId());
        }
    }
}
