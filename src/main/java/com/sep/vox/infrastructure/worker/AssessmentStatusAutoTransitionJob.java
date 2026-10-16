package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;
import com.sep.vox.application.port.input.service.ZeroScoreAssessmentResultService;
import com.sep.vox.application.port.input.usecase.assessment.AssessmentQuestionSecureLockService;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentPaperStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentPaperRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

@Component
public class AssessmentStatusAutoTransitionJob {

    private static final Logger log = LoggerFactory.getLogger(AssessmentStatusAutoTransitionJob.class);

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final ZeroScoreAssessmentResultService zeroScoreAssessmentResultService;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService;
    private final AssessmentScheduleClosureService assessmentScheduleClosureService;

    public AssessmentStatusAutoTransitionJob(
            AssessmentRepository assessmentRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentQuestionSecureLockService assessmentQuestionSecureLockService,
            ZeroScoreAssessmentResultService zeroScoreAssessmentResultService,
            ClassTestGradingAssignmentService classTestGradingAssignmentService,
            AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService,
            AssessmentScheduleClosureService assessmentScheduleClosureService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentPaperRepository = assessmentPaperRepository;
        this.assessmentQuestionSecureLockService = assessmentQuestionSecureLockService;
        this.zeroScoreAssessmentResultService = zeroScoreAssessmentResultService;
        this.classTestGradingAssignmentService = classTestGradingAssignmentService;
        this.assessmentHumanGradingNotificationService = assessmentHumanGradingNotificationService;
        this.assessmentScheduleClosureService = assessmentScheduleClosureService;
    }

    @Scheduled(fixedDelay = 60000)
    public void run() {
        var now = Instant.now();

        for (var assessment : assessmentRepository.findByStatusAndOpenAtBefore(AssessmentStatus.SCHEDULED, now)) {
            if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
                continue;
            }
            assessment.setStatus(AssessmentStatus.IN_PROGRESS);
            assessment.setUpdatedAt(now);
            assessmentRepository.save(assessment);
            // Khớp đúng side-effect của UpdateAssessmentStatusUseCase.START — khoá paper khi bài chính thức mở cho học sinh,
            // để không lệch hành vi giữa mở tự động (openAt) và CHAIR bấm tay.
            for (var paper : assessmentPaperRepository.findByAssessmentId(assessment.getId())) {
                if (paper.getStatus() != AssessmentPaperStatus.LOCKED) {
                    paper.setStatus(AssessmentPaperStatus.LOCKED);
                    paper.setUpdatedAt(now);
                    assessmentPaperRepository.save(paper);
                }
            }
            log.info("Tự động mở bài kiểm tra {} (openAt đã tới)", assessment.getId());
        }

        for (var assessment : assessmentRepository.findByStatusAndCloseAtBefore(AssessmentStatus.IN_PROGRESS, now)) {
            if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
                continue;
            }
            // Bình thường guard không bao giờ chặn ở đây: closeAt luôn ở sau endDate của mọi ca
            // (requireClassTestScheduleWindow bảo đảm). Nhánh này phòng dữ liệu lệch, và continue
            // khiến nó tự lành ở tick sau thay vì cắt ngang buổi thi đang chạy.
            try {
                assessmentScheduleClosureService.requireNoActiveSessionInOngoingSchedule(assessment.getId(), now);
            } catch (IllegalStateException ex) {
                log.info("Hoãn tự đóng bài kiểm tra {}: {}", assessment.getId(), ex.getMessage());
                continue;
            }
            assessment.setStatus(AssessmentStatus.CLOSED);
            assessment.setUpdatedAt(now);
            assessmentRepository.save(assessment);
            // Khớp đúng side-effect của UpdateAssessmentStatusUseCase.CLOSE: đa số bài trên lớp đóng bằng
            // đường này, bỏ sót ở đây là ca thi ở lại PUBLISHED vĩnh viễn.
            assessmentScheduleClosureService.closeSchedulesForAssessment(assessment.getId(), null, now);
            assessmentQuestionSecureLockService.releaseIfAutoAfterClose(assessment.getId());
            zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
            // Đa số bài trên lớp đóng bằng đường này chứ không phải CHAIR bấm tay — bỏ sót
            // ở đây là mất phân công chấm cho gần như toàn bộ bài.
            classTestGradingAssignmentService.ensureAssignmentsForAssessment(assessment.getId());
            // Khớp side-effect của UpdateAssessmentStatusUseCase.CLOSE: đa số bài trên lớp đóng bằng
            // đường này, bỏ sót ở đây là giáo viên không bao giờ được báo có bài chờ chấm.
            assessmentHumanGradingNotificationService.publishIfPendingReview(assessment, now);
            log.info("Tự động đóng bài kiểm tra {} (closeAt đã tới)", assessment.getId());
        }
    }
}
