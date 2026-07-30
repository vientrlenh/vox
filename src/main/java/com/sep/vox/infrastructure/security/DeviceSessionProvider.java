package com.sep.vox.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;

@Component
public class DeviceSessionProvider implements SessionManagerPort {

    private final DeviceSessionRepository deviceSessionRepository;

    public DeviceSessionProvider(DeviceSessionRepository deviceSessionRepository) {
        this.deviceSessionRepository = deviceSessionRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(UUID sessionId, Instant now) {
        deviceSessionRepository.revokeDeviceSession(sessionId, now);
    }
    
}
