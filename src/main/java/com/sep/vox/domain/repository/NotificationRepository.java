package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.notification.Notification;

public interface NotificationRepository {
    Optional<Notification> findById(UUID id);
    Notification save(Notification notification);
}
