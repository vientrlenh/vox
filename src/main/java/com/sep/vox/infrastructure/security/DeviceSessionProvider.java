package com.sep.vox.infrastructure.security;

import com.sep.vox.domain.model.devicesession.DeviceSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.DeviceSessionRevokedEvent;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;

@Component
@RequiredArgsConstructor
public class DeviceSessionProvider implements SessionManagerPort {

    private final DeviceSessionRepository deviceSessionRepository;
    private final EventPublisherPort eventPublisherPort;

    /**
     * Runs in its own transaction (REQUIRES_NEW) so the revoke survives the caller's rollback: RefreshUseCase
     * revokes the session on a reused refresh token or a mismatched device id and then throws
     * UnauthorizedException, which rolls back its own transaction. Joining that transaction would undo the revoke.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(UUID sessionId, Instant now) {
        // Read before revoking: userId + deviceId are needed to remove the device that receives notifications,
        // and the record still exists after revoke so the order is not mandatory -- reading first is only
        // so we do not depend on whether revoke keeps the row intact.
        Optional<DeviceSession> session = deviceSessionRepository.findById(sessionId);

        int revoked = deviceSessionRepository.revokeDeviceSession(sessionId, now);
        if (revoked == 0 || session.isEmpty()) {
            return;
        }

        eventPublisherPort.publish(new DeviceSessionRevokedEvent(
            sessionId, session.get().getUserId(), session.get().getDeviceId(), now));
    }
}
