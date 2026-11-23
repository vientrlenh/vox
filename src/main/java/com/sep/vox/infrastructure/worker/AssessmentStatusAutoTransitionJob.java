package com.sep.vox.infrastructure.worker;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentPaper;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentStatusAutoTransitionJob {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentQuestionSecureLockService assessmentQuestionSecureLockService;
    private final ZeroScoreAssessmentResultService zeroScoreAssessmentResultService;
    private final ClassTestGradingAssignmentService classTestGradingAssignmentService;
    private final AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService;
    private final AssessmentScheduleClosureService assessmentScheduleClosureService;

    @Scheduled(fixedDelay = 60000)
    public void run() {
        Instant now = Instant.now();

        for (Assessment assessment : assessmentRepository.findByStatusAndOpenAtBefore(AssessmentStatus.SCHEDULED, now)) {
            if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
                continue;
            }
            assessment.setStatus(AssessmentStatus.IN_PROGRESS);
            assessmentRepository.save(assessment);
            // Matches exactly the side-effect of UpdateAssessmentStatusUseCase.START — locks the paper when the official assessment opens for students,
            // so behavior does not drift between the automatic opening (openAt) and the CHAIR pressing it manually.
            for (AssessmentPaper paper : assessmentPaperRepository.findByAssessmentId(assessment.getId())) {
                if (paper.getStatus() != AssessmentPaperStatus.LOCKED) {
                    paper.setStatus(AssessmentPaperStatus.LOCKED);
                    paper.setUpdatedAt(now);
                    assessmentPaperRepository.save(paper);
                }
            }
            log.info("Tự động mở bài kiểm tra {} (openAt đã tới)", assessment.getId());
        }

        for (Assessment assessment : assessmentRepository.findByStatusAndCloseAtBefore(AssessmentStatus.IN_PROGRESS, now)) {
            if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
                continue;
            }
            // Normally the guard never blocks here: closeAt is always after the endDate of every shift
            // (guaranteed by requireClassTestScheduleWindow). This branch protects against inconsistent data, and the continue
            // makes it heal itself on the next tick instead of cutting into an assessment that is running.
            try {
                assessmentScheduleClosureService.requireNoActiveSessionInOngoingSchedule(assessment.getId(), now);
            } catch (IllegalArgumentException ex) {
                log.info("Hoãn tự đóng bài kiểm tra {}: {}", assessment.getId(), ex.getMessage());
                continue;
            }
            assessment.setStatus(AssessmentStatus.CLOSED);
            assessmentRepository.save(assessment);
            // Matches exactly the side-effect of UpdateAssessmentStatusUseCase.CLOSE: most class tests are closed through
            // this path, and missing it here leaves the assessment shift in PUBLISHED forever.
            assessmentScheduleClosureService.closeSchedulesForAssessment(assessment.getId(), now);
            assessmentQuestionSecureLockService.releaseIfAutoAfterClose(assessment.getId());
            zeroScoreAssessmentResultService.ensureZeroResultsForMissingOrEmptyAttempts(assessment.getId());
            // Most class tests are closed through this path rather than by the CHAIR manually — missing
            // it here means losing the grading assignments for almost all attempts.
            classTestGradingAssignmentService.ensureAssignmentsForAssessment(assessment.getId());
            // Matches the side-effect of UpdateAssessmentStatusUseCase.CLOSE: most class tests are closed through
            // this path, and missing it here means teachers are never told there is an attempt waiting for grading.
            assessmentHumanGradingNotificationService.publishIfPendingReview(assessment, now);
            log.info("Tự động đóng bài kiểm tra {} (closeAt đã tới)", assessment.getId());
        }
    }
}
