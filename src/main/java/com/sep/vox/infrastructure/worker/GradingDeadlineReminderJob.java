package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reminds teachers when an assignment is approaching its deadline.
 *
 * <p>Duplicate reminders are prevented by the {@code reminded_at} column and not by the run schedule: the job runs
 * every 15 minutes and may run again after a restart, so "has it been reminded" must live in the
 * data. Across multiple instances the guard is {@code FOR UPDATE SKIP LOCKED} in
 * {@code findDueForReminder} -- {@code reminded_at} alone only prevents duplicates across
 * RUNS, not across processes running in parallel.
 *
 * <p>The threshold is {@value #REMIND_BEFORE_HOURS} hours before the deadline: early enough to still grade in time,
 * late enough not to remind about work that was just assigned.
 *
 * <p><strong>Why it loops by batch:</strong> the select has a {@code LIMIT} so that one transaction
 * does not take thousands of rows. If each run did exactly one batch, a large backlog would
 * drain at a rate of one batch per 15 minutes, and the "remind before the deadline" mail could arrive AFTER the deadline has passed --
 * exactly what this job exists to avoid. So it runs here until the work is done, one separate
 * transaction per batch, with a cap of {@value #MAX_PASSES_PER_RUN} batches so one run does not go on
 * forever if some row keeps coming back.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GradingDeadlineReminderJob {

    private static final int REMIND_BEFORE_HOURS = 24;
    private static final int MAX_PASSES_PER_RUN = 20;

    private final GradingDeadlineReminderBatch gradingDeadlineReminderBatch;

    @Scheduled(fixedDelay = 900000)
    public void remind() {
        // The cutoff is fixed ONCE for the whole run: recomputing it per batch would make the window drift and
        // pull in assignments that are not yet due for a reminder.
        Instant threshold = Instant.now().plus(Duration.ofHours(REMIND_BEFORE_HOURS));
        int total = remindInBatches(threshold);

        if (total > 0) {
            log.info("Sent grading deadline reminders for {} assignments", total);
        }
    }

    private int remindInBatches(Instant threshold) {
        int total = 0;

        for (int pass = 0; pass < MAX_PASSES_PER_RUN; pass++) {
            int reminded;
            try {
                reminded = gradingDeadlineReminderBatch.remindOnce(threshold);
            } catch (Exception e) {
                // The next batch will also fail for the same cause; stop and let the run 15 minutes later
                // try again instead of spinning on one error.
                log.error("Error while sending grading deadline reminders, stopping this run", e);
                break;
            }
            if (reminded == 0) {
                break;
            }
            total += reminded;
        }

        return total;
    }
}
