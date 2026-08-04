package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.notification.Notification;
import com.sep.vox.infrastructure.persistence.entity.NotificationJpaEntity;

public final class NotificationMapper {

    private NotificationMapper() {}

    public static Notification toDomain(NotificationJpaEntity jpa) {
        return new Notification(
            jpa.getId(),
            jpa.getUserId(),
            jpa.getEventId(),
            jpa.getEventType(),
            jpa.getTitle(),
            jpa.getBody(),
            jpa.getPayload(),
            jpa.getReadAt(),
            jpa.getCreatedAt()
        );
    }

    public static NotificationJpaEntity toJpa(Notification notification) {
        return new NotificationJpaEntity(
            notification.getId(),
            notification.getUserId(),
            notification.getEventId(),
            notification.getEventType(),
            notification.getTitle(),
            notification.getBody(),
            notification.getPayload(),
            notification.getReadAt(),
            notification.getCreatedAt()
        );
    }
}
