package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.ImportCommitService;
import com.sep.vox.application.queue.ImportQueueService;

@Component
public class ImportJobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ImportJobDispatcher.class);
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
        var free = workers.getMaxPoolSize() - workers.getActiveCount();
        if (free <= 0) {
            return;
        }
        for (var id : queueService.claimQueued(instanceId, free, LEASE)) {
            try {
                workers.execute(() -> safeCommit(id));
            } catch (Exception e) {
                // pool đầy/từ chối: để job ở IMPORTING -> sweeper sẽ requeue khi lease hết
                log.warn("Không submit được job import {}: {}", id, e.getMessage());
            }
        }
    }

    private void safeCommit(UUID id) {
        try {
            commitService.commit(id);
        } catch (Exception e) {
            // commit() đã tự set FAILED cho lỗi trong handler; đây là lỗi ngoài dự kiến
            log.error("Lỗi khi commit phiên import {}", id, e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void sweep() {
        try {
            var requeued = queueService.requeueExpired(MAX_ATTEMPTS);
            if (requeued > 0) {
                log.info("Sweeper requeue {} phiên import quá hạn lease", requeued);
            }
        } catch (Exception e) {
            log.error("Lỗi khi sweep phiên import quá hạn", e);
        }
    }
}
