package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;

public interface NotificationDeviceRepository {
    Optional<NotificationDevice> findById(UUID id);
    NotificationDevice save(NotificationDevice notificationDevice);

    List<NotificationDevice> findByUserId(UUID userId);

    /** Dọn FID mà FCM đã báo là không còn dùng được. */
    int deleteByInstallationIdIn(Collection<String> installationIds);
    int deleteByUserIdAndDeviceIdAndExceptInstallationId(UUID userId, String deviceId, String installationId);
    int deleteByUserIdAndInstallationId(UUID userId, String installationId);
    int deleteByUserIdAndDeviceId(UUID userId, String deviceId);
    int deleteByLastSeenAtBefore(Instant threshold);
    int registerDevice(UUID userId, String deviceId, NotificationDevicePlatform platform, String installationId, Instant now);
}
