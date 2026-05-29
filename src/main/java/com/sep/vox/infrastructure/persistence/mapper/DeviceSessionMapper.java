package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.infrastructure.persistence.entity.DeviceSessionJpaEntity;

public final class DeviceSessionMapper {
    
    public static DeviceSession toDomain(DeviceSessionJpaEntity jpa) {
        return new DeviceSession(
            jpa.getId(), 
            jpa.getUserId(), 
            jpa.getDeviceId(), 
            jpa.getDeviceName(), 
            sessionPlatform(jpa.getPlatform()), 
            jpa.getIpAddress(), 
            jpa.getUserAgent(), 
            jpa.getRevokedAt()
        );
    }

    public static DeviceSessionJpaEntity toJpa(DeviceSession session) {
        return new DeviceSessionJpaEntity(
            session.getId(), 
            session.getUserId(), 
            session.getDeviceId(), 
            session.getDeviceName(), 
            valueOf(session.getPlatform()), 
            session.getIpAddress(), 
            session.getUserAgent(), 
            session.getRevokedAt()
        );
    }

    private static SessionPlatform sessionPlatform(String platform) {
        return platform == null ? null : SessionPlatform.valueOf(platform);
    }

    private static String valueOf(SessionPlatform platform) {
        return platform == null ? null : platform.name();
    }
}
