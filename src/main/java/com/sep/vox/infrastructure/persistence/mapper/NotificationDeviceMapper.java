package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.infrastructure.persistence.entity.NotificationDeviceJpaEntity;

public final class NotificationDeviceMapper {

    private NotificationDeviceMapper() {}

    public static NotificationDevice toDomain(NotificationDeviceJpaEntity jpa) {
        return new NotificationDevice(
            jpa.getId(),
            jpa.getUserId(),
            jpa.getDeviceId(),
            platformFromString(jpa.getPlatform()),
            jpa.getInstallationId(),
            jpa.getCreatedAt(),
            jpa.getLastSeenAt()
        );
    }

    public static NotificationDeviceJpaEntity toJpa(NotificationDevice device) {
        return new NotificationDeviceJpaEntity(
            device.getId(),
            device.getUserId(),
            device.getDeviceId(),
            valueOf(device.getPlatform()),
            device.getInstallationId(),
            device.getCreatedAt(),
            device.getLastSeenAt()
        );
    }

    private static NotificationDevicePlatform platformFromString(String platform) {
        return platform == null ? null : NotificationDevicePlatform.valueOf(platform);
    }

    private static String valueOf(NotificationDevicePlatform platform) {
        return platform == null ? null : platform.name();
    }
}
