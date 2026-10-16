package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.notification.Notification;

public record NotificationDto(
    UUID id,
    UUID userId,
    UUID eventId,
    String eventType,
    String title,
    String body,
    String payload,
    Instant readAt,
    Instant createdAt
) {

    public static NotificationDto toDto(Notification notification) {
        return new NotificationDto(
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

    public static List<NotificationDto> toDtoList(List<Notification> notifications) {
        return notifications.stream()
            .map(NotificationDto::toDto)
            .toList();
    }
}
