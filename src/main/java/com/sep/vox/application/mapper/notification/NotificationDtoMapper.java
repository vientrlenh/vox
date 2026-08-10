package com.sep.vox.application.mapper.notification;

import java.time.Instant;
import java.util.List;

import com.sep.vox.application.query.dto.NotificationDto;
import com.sep.vox.domain.model.notification.Notification;

public final class NotificationDtoMapper {

    private NotificationDtoMapper() {}

    public static NotificationDto toNotificationDto(Notification notification) {
        return new NotificationDto(
            notification.getId(),
            notification.getUserId(),
            notification.getEventId(),
            notification.getEventType(),
            notification.getTitle(),
            notification.getBody(),
            notification.getPayload(),
            format(notification.getReadAt()),
            format(notification.getCreatedAt())
        );
    }

    public static List<NotificationDto> toNotificationDtoList(List<Notification> notifications) {
        return notifications.stream()
            .map(NotificationDtoMapper::toNotificationDto)
            .toList();
    }

    private static String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
