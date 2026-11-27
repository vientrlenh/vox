package com.sep.vox.infrastructure.worker;

import com.sep.vox.domain.model.assessment.Assessment;
import java.time.Instant;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.AssessmentHumanGradingNotificationService;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Sweeps assessments that have attempts "Waiting for AI score review" and have never been reminded, then notifies the person in charge.
 *
 * <p>Runs WHILE the assessment is STILL RUNNING rather than waiting for it to close: a session that ends early already has
 * results, and making the grader wait until closing wastes most of the grading time.
 * For an assessment lasting several days that wait is several days.
 *
 * <p>An assessment is reminded at most ONCE: the guard is {@code assessments.human_grading_notified_at},
 * stamped by the service itself in the same transaction as the outbox. The query already filters rows not yet reminded
 * so a normal sweep reads nothing -- see
 * {@code SpringDataAssessmentRepository.findDueForHumanGradingNotice}.
 *
 * <p>Kept apart from {@link AssessmentStatusAutoTransitionJob} even though they share a tick: that job only touches assessments due to close,
 * while this sweep has to look at running assessments too. Merging them would force that job to widen its sweep scope just because
 * they share one clock.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentHumanGradingNoticeBatch {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentHumanGradingNotificationService assessmentHumanGradingNotificationService;

    /**
     * Not {@code @Transactional} here: each attempt must be its own transaction of
     * {@code publishIfPendingReview}. Wrapping the whole run in one transaction means one failing attempt
     * rolls back the marking of all the attempts already processed before it, and the next run reminds from
     * the start.
     */
    @Scheduled(fixedDelay = 60000)
    public void run() {
        List<Assessment> due = assessmentRepository.findDueForHumanGradingNotice();
        if (due.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        int notified = 0;
        for (Assessment assessment : due) {
            try {
                assessmentHumanGradingNotificationService.publishIfPendingReview(assessment, now);
                notified++;
            } catch (Exception e) {
                // One failing attempt (lost recipient, inconsistent data) must not cut off the rest
                // of the sweep. The marker is not closed so this attempt will be retried on the next tick.
                log.error("Could not send the manual grading notice for assessment {}", assessment.getId(), e);
            }
        }

        log.info("Manual grading notice sweep: {}/{} assessments notified", notified, due.size());
    }
}
