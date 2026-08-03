package com.sep.vox.infrastructure.worker.personalization;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.PracticeSessionCleanupService;

@Component
public class PracticeSessionHeartbeatCleanupJob {

    private final PracticeSessionCleanupService cleanupService;

    public PracticeSessionHeartbeatCleanupJob(
            PracticeSessionCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Scheduled(fixedDelayString = "${app.practice.heartbeat-cleanup-ms:300000}")
    public void cleanup() {
        cleanupService.cleanupStaleSessions(
            Instant.now().minus(Duration.ofMinutes(3))
        );
    }
}
