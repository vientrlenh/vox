package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.devicesession.DeviceSession;

public interface DeviceSessionRepository {
    DeviceSession save(DeviceSession session);
    Optional<DeviceSession> findById(UUID id);
    List<DeviceSession> findByUserId(UUID userId);
    int revokeDeviceSession(UUID id, OffsetDateTime now);
    void updatePushToken(UUID userId, String deviceId, String pushToken);
    List<DeviceSession> findActivePushTokensByUserId(UUID userId);
}
