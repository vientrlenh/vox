package com.sep.vox.infrastructure.worker;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.AssessmentScheduleClosureService;

/**
 * A published session whose end time has passed automatically becomes COMPLETED. This is the normal path: a session
 * closes when its time runs out, not waiting until the whole class test closes.
 *
 * <p>Kept apart from {@code AssessmentStatusAutoTransitionJob} because that job only writes {@code AssessmentStatus} and deliberately skips
 * centralized assessments, while the session sweep must run for both kinds.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentScheduleAutoCompleteJob {

    /**
     * Ceiling per run. The first run after deployment has to sweep the historical backlog too (every session whose time
     * ran out earlier is still in PUBLISHED), so it is spread out gradually instead of swallowed in one go.
     */
    private static final int BATCH_SIZE = 200;

    private final AssessmentScheduleClosureService assessmentScheduleClosureService;

    @Scheduled(fixedDelay = 60000)
    public void run() {
        int completed = assessmentScheduleClosureService.completeEndedSchedules(Instant.now(), BATCH_SIZE);
        if (completed > 0) {
            log.info("Tự đóng {} ca thi đã qua giờ kết thúc", completed);
        }
    }
}
