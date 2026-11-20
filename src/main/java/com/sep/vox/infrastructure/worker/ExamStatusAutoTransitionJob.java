package com.sep.vox.infrastructure.worker;

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
        var now = Instant.now();

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
            // Bình thường guard không bao giờ chặn ở đây: closeAt luôn ở sau endDate của mọi ca
            // (requireClassTestScheduleWindow bảo đảm). Nhánh này phòng dữ liệu lệch, và continue
            // khiến nó tự lành ở tick sau thay vì cắt ngang buổi thi đang chạy.
            try {
                examScheduleClosureService.requireNoActiveSessionInOngoingSchedule(exam.getId(), now);
            } catch (IllegalStateException ex) {
                log.info("Hoãn tự đóng bài kiểm tra {}: {}", exam.getId(), ex.getMessage());
                continue;
            }
            exam.setStatus(ExamStatus.CLOSED);
            exam.setUpdatedAt(now);
            examRepository.save(exam);
            // Khớp đúng side-effect của UpdateExamStatusUseCase.CLOSE: đa số bài trên lớp đóng bằng
            // đường này, bỏ sót ở đây là ca thi ở lại PUBLISHED vĩnh viễn.
            examScheduleClosureService.closeSchedulesForExam(exam.getId(), null, now);
            examQuestionSecureLockService.releaseIfAutoAfterClose(exam.getId());
            zeroScoreExamResultService.ensureZeroResultsForMissingOrEmptyAttempts(exam.getId());
            // Đa số bài trên lớp đóng bằng đường này chứ không phải CHAIR bấm tay — bỏ sót
            // ở đây là mất phân công chấm cho gần như toàn bộ bài.
            classTestGradingAssignmentService.ensureAssignmentsForExam(exam.getId());
            // Khớp side-effect của UpdateExamStatusUseCase.CLOSE: đa số bài trên lớp đóng bằng
            // đường này, bỏ sót ở đây là giáo viên không bao giờ được báo có bài chờ chấm.
            examHumanGradingNotificationService.publishIfPendingReview(exam, now);
            log.info("Tự động đóng bài kiểm tra {} (closeAt đã tới)", exam.getId());
        }
    }
}
