package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.repository.NotificationDeviceRepository;

/**
 * FCM reports UNREGISTERED only when a push is really sent to that device, so a device that
 * disappears silently -- app uninstalled without logging out, a changed phone, cleared browser data --
 * would never leave the table on its own. This job is the only cleanup path for that group.
 */
@Component
@Slf4j
public class NotificationDeviceCleanUpJob {

    private final NotificationDeviceRepository notificationDeviceRepository;
    private final int retentionDays;

    public NotificationDeviceCleanUpJob(
            NotificationDeviceRepository notificationDeviceRepository,
            @Value("${app.notification.device-retention-days:90}") int retentionDays) {
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelay = 86_400_000, initialDelay = 300_000)
    public void deleteStaleDevices() {
        if (retentionDays <= 0) {
            return;
        }

        Instant threshold = Instant.now().minus(Duration.ofDays(retentionDays));
        int removed = notificationDeviceRepository.deleteByLastSeenAtBefore(threshold);
        if (removed > 0) {
            log.info("Cleaned up {} devices inactive for more than {} days", removed, retentionDays);
        }
    }
}
