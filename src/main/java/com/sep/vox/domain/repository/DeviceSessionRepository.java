package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.devicesession.DeviceSession;

public interface DeviceSessionRepository {
    DeviceSession save(DeviceSession session);
    Optional<DeviceSession> findById(UUID id);
    List<DeviceSession> findByUserId(UUID userId);
}
