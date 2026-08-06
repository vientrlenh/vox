package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.notification.NotificationDevice;

public interface NotificationDeviceRepository {
    Optional<NotificationDevice> findById(UUID id);
    NotificationDevice save(NotificationDevice notificationDevice);
}
