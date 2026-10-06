package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.platform.Platform;
import com.sep.vox.infrastructure.persistence.entity.DeviceSessionJpaEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public final class DeviceSessionMapper {

    private DeviceSessionMapper() {}
    
    public static DeviceSession toDomain(DeviceSessionJpaEntity jpa) {
        try {
            return DeviceSession.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .deviceId(jpa.getDeviceId())
                .deviceName(jpa.getDeviceName())
                .platform(Platform.from(jpa.getPlatform()))
                .ipAddress(jpa.getIpAddress())
                .userAgent(jpa.getUserAgent())
                .createdAt(jpa.getCreatedAt())
                .revokedAt(jpa.getRevokedAt())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping device session entity to model: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }

    public static DeviceSessionJpaEntity toJpa(DeviceSession session) {
        try {
            return DeviceSessionJpaEntity.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .deviceId(session.getDeviceId())
                .deviceName(session.getDeviceName())
                .platform(Platform.value(session.getPlatform()))
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .createdAt(session.getCreatedAt())
                .revokedAt(session.getRevokedAt())
                .build();
        } catch (Exception ex) {
            log.error("An error occurred when mapping device session model to entity: {}", ex.getMessage(), ex);
            throw new IllegalStateException("A problem occurred when converting entity to model");
        }

    }
}
