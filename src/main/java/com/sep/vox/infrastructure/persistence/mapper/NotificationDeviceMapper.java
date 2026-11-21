package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.infrastructure.persistence.entity.notification.NotificationDeviceJpaEntity;

public final class NotificationDeviceMapper {

    private NotificationDeviceMapper() {}

    public static NotificationDevice toDomain(NotificationDeviceJpaEntity jpa) {
        return NotificationDevice.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .deviceId(jpa.getDeviceId())
                .platform(platformFromString(jpa.getPlatform()))
                .installationId(jpa.getInstallationId())
                .createdAt(jpa.getCreatedAt())
                .lastSeenAt(jpa.getLastSeenAt())
                .build();
    }

    public static NotificationDeviceJpaEntity toJpa(NotificationDevice device) {
        return NotificationDeviceJpaEntity.builder()
                .id(device.getId())
                .userId(device.getUserId())
                .deviceId(device.getDeviceId())
                .platform(valueOf(device.getPlatform()))
                .installationId(device.getInstallationId())
                .createdAt(device.getCreatedAt())
                .lastSeenAt(device.getLastSeenAt())
                .build();
    }

    private static NotificationDevicePlatform platformFromString(String platform) {
        return platform == null ? null : NotificationDevicePlatform.valueOf(platform);
    }

    private static String valueOf(NotificationDevicePlatform platform) {
        return platform == null ? null : platform.name();
    }
}
