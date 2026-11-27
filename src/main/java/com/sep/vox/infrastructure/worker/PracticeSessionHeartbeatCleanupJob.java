package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.PracticeGradingFlushService;
import com.sep.vox.application.port.input.service.PracticeSessionCleanupService;

@Component
@RequiredArgsConstructor
public class PracticeSessionHeartbeatCleanupJob {

    /**
     * Only re-sweeps sessions closed within the last 24h. If grading is truly broken, an unbounded sweep
     * would fire the same set of events every 5 minutes forever -- past the window we stop, and the summary screen says
     * plainly that grading did not finish instead of retrying again.
     */
    private static final Duration ORPHAN_SWEEP_WINDOW = Duration.ofHours(24);

    private final PracticeSessionCleanupService cleanupService;
    private final PracticeGradingFlushService gradingFlushService;

    /**
     * Widened from 3 -> 10 minutes (2026-08-12). Three minutes is too short for SPEAKING practice: the AI reading the question alone takes nearly
     * a minute, then the student thinks and speaks for another minute or two. Combined with the fact that nobody used to update
     * last_heartbeat_at, this threshold killed EVERY normal session at minute 3.
     *
     * <p>Now agents send a heartbeat every 60 seconds so 10 minutes is very generous: a truly dead session is still cleaned up after
     * 10 minutes, while a running session would have to miss 10 consecutive beats to be touched.
     */
    private static final Duration STALE_AFTER = Duration.ofMinutes(10);

    @Scheduled(fixedDelayString = "${app.practice.heartbeat-cleanup-ms:300000}")
    public void cleanup() {
        cleanupService.cleanupStaleSessions(
            Instant.now().minus(STALE_AFTER)
        );
        // Orphans are swept only after the stale sessions are closed: a session just closed above has also had its
        // grading flush, so this round is left with exactly the turns that really slipped through.
        gradingFlushService.sweepEndedSessions(Instant.now().minus(ORPHAN_SWEEP_WINDOW));
    }
}
