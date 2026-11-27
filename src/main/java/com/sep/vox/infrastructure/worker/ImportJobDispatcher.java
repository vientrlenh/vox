package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.ImportCommitService;
import com.sep.vox.application.port.input.service.ImportQueueService;

@Component
@Slf4j
public class ImportJobDispatcher {

    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 3;

    private final ImportQueueService queueService;
    private final ImportCommitService commitService;
    private final ThreadPoolTaskExecutor workers;
    private final UUID instanceId = UUID.randomUUID();

    public ImportJobDispatcher(
            ImportQueueService queueService,
            ImportCommitService commitService,
            @Qualifier("fileImportExecutor") ThreadPoolTaskExecutor workers) {
        this.queueService = queueService;
        this.commitService = commitService;
        this.workers = workers;
    }

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        int free = workers.getMaxPoolSize() - workers.getActiveCount();
        if (free <= 0) {
            return;
        }
        for (UUID id : queueService.claimQueued(instanceId, free, LEASE)) {
            try {
                workers.execute(() -> safeCommit(id));
            } catch (Exception e) {
                // pool full/rejected: leave the job in IMPORTING -> the sweeper will requeue it when the lease expires
                log.warn("Could not submit import job {}: {}", id, e.getMessage());
            }
        }
    }

    private void safeCommit(UUID id) {
        try {
            commitService.commit(id);
        } catch (Exception e) {
            // commit() already sets FAILED for errors in the handler; this is an unexpected error
            log.error("Error while committing import session {}", id, e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void sweep() {
        try {
            int requeued = queueService.requeueExpired(MAX_ATTEMPTS);
            if (requeued > 0) {
                log.info("Sweeper requeued {} import sessions past their lease", requeued);
            }
        } catch (Exception e) {
            log.error("Error while sweeping expired import sessions", e);
        }
    }
}
