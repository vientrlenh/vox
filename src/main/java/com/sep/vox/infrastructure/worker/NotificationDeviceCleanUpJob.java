package com.sep.vox.infrastructure.worker;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.repository.NotificationDeviceRepository;

/**
 * FCM chỉ báo UNREGISTERED khi có lần push thật sự gửi tới thiết bị đó, nên thiết bị
 * biến mất lặng lẽ -- gỡ app mà không đăng xuất, đổi máy, xoá dữ liệu trình duyệt --
 * sẽ không bao giờ tự rời khỏi bảng. Job này là đường dọn duy nhất cho nhóm đó.
 */
@Component
public class NotificationDeviceCleanUpJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeviceCleanUpJob.class);

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

        var threshold = Instant.now().minus(Duration.ofDays(retentionDays));
        var removed = notificationDeviceRepository.deleteByLastSeenAtBefore(threshold);
        if (removed > 0) {
            LOGGER.info("Đã dọn {} thiết bị không hoạt động quá {} ngày", removed, retentionDays);
        }
    }
}
