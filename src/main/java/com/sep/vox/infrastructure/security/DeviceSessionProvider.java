package com.sep.vox.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.DeviceSessionRevokedEvent;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;

@Component
public class DeviceSessionProvider implements SessionManagerPort {

    private final DeviceSessionRepository deviceSessionRepository;
    private final EventPublisherPort eventPublisherPort;

    public DeviceSessionProvider(
            DeviceSessionRepository deviceSessionRepository,
            EventPublisherPort eventPublisherPort) {
        this.deviceSessionRepository = deviceSessionRepository;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(UUID sessionId, Instant now) {
        // Đọc trước khi thu hồi: cần userId + deviceId để gỡ thiết bị nhận thông báo,
        // và bản ghi vẫn còn sau khi revoke nên thứ tự không bắt buộc -- đọc trước chỉ
        // để không phụ thuộc vào việc revoke có giữ nguyên dòng hay không.
        var session = deviceSessionRepository.findById(sessionId);

        var revoked = deviceSessionRepository.revokeDeviceSession(sessionId, now);
        if (revoked == 0 || session.isEmpty()) {
            return;
        }

        eventPublisherPort.publish(new DeviceSessionRevokedEvent(
            sessionId, session.get().getUserId(), session.get().getDeviceId(), now));
    }
}
