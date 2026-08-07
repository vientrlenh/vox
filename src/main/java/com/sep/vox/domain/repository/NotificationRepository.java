package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.notification.Notification;

public interface NotificationRepository {
    Optional<Notification> findById(UUID id);
    Notification save(Notification notification);
    Optional<Notification> saveIfAbsent(Notification notification);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    List<Notification> findByUserIdOrderByIdDesc(UUID userId, int limit);
    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(UUID userId, UUID cursor, int limit);
    long countUnreadByUserId(UUID userId);

    int markRead(UUID userId, UUID notificationId, Instant now);
    int markAllRead(UUID userId, Instant now);
}
