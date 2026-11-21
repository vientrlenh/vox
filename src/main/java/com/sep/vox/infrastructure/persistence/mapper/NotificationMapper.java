package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.infrastructure.persistence.entity.notification.NotificationJpaEntity;

public final class NotificationMapper {

    private NotificationMapper() {}

    public static Notification toDomain(NotificationJpaEntity jpa) {
        return Notification.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .eventId(jpa.getEventId())
                .eventType(jpa.getEventType())
                .title(jpa.getTitle())
                .body(jpa.getBody())
                .payload(jpa.getPayload())
                .readAt(jpa.getReadAt())
                .createdAt(jpa.getCreatedAt())
                .build();
    }

    public static NotificationJpaEntity toJpa(Notification notification) {
        return NotificationJpaEntity.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .eventId(notification.getEventId())
                .eventType(notification.getEventType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .payload(notification.getPayload())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
