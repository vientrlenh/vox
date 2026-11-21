package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.infrastructure.persistence.entity.devicesession.DeviceSessionJpaEntity;

public final class DeviceSessionMapper {
    
    public static DeviceSession toDomain(DeviceSessionJpaEntity jpa) {
        return DeviceSession.builder()
            .id(jpa.getId())
            .userId(jpa.getUserId())
            .deviceId(jpa.getDeviceId())
            .deviceName(jpa.getDeviceName())
            .platform(sessionPlatform(jpa.getPlatform()))
            .ipAddress(jpa.getIpAddress())
            .userAgent(jpa.getUserAgent())
            .revokedAt(jpa.getRevokedAt())
            .build();
    }

    public static DeviceSessionJpaEntity toJpa(DeviceSession session) {
        return DeviceSessionJpaEntity.builder()
            .id(session.getId())
            .userId(session.getUserId())
            .deviceId(session.getDeviceId())
            .deviceName(session.getDeviceName())
            .platform(valueOf(session.getPlatform()))
            .ipAddress(session.getIpAddress())
            .userAgent(session.getUserAgent())
            .revokedAt(session.getRevokedAt())
            .build();
    }

    private static SessionPlatform sessionPlatform(String platform) {
        return platform == null ? null : SessionPlatform.valueOf(platform);
    }

    private static String valueOf(SessionPlatform platform) {
        return platform == null ? null : platform.name();
    }
}
