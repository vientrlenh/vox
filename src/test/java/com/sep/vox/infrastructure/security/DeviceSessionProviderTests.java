package com.sep.vox.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.event.DeviceSessionRevokedEvent;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.repository.DeviceSessionRepository;

class DeviceSessionProviderTests {

    private DeviceSessionRepository deviceSessionRepository;
    private EventPublisherPort eventPublisherPort;
    private DeviceSessionProvider provider;

    private UUID sessionId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        deviceSessionRepository = mock(DeviceSessionRepository.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        provider = new DeviceSessionProvider(deviceSessionRepository, eventPublisherPort);

        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void should_publish_event_with_user_and_device_of_the_revoked_session() {
        when(deviceSessionRepository.findById(sessionId)).thenReturn(Optional.of(session("lab-01")));
        when(deviceSessionRepository.revokeDeviceSession(any(), any())).thenReturn(1);

        var now = Instant.now();
        provider.revoke(sessionId, now);

        var captor = ArgumentCaptor.forClass(DeviceSessionRevokedEvent.class);
        verify(eventPublisherPort).publish(captor.capture());

        assertThat(captor.getValue().sessionId()).isEqualTo(sessionId);
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().deviceId()).isEqualTo("lab-01");
        assertThat(captor.getValue().revokedAt()).isEqualTo(now);
    }

    @Test
    void should_not_publish_when_session_was_already_revoked() {
        when(deviceSessionRepository.findById(sessionId)).thenReturn(Optional.of(session("lab-01")));
        when(deviceSessionRepository.revokeDeviceSession(any(), any())).thenReturn(0);

        provider.revoke(sessionId, Instant.now());

        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void should_not_publish_when_session_does_not_exist() {
        when(deviceSessionRepository.findById(sessionId)).thenReturn(Optional.empty());
        when(deviceSessionRepository.revokeDeviceSession(any(), any())).thenReturn(0);

        provider.revoke(sessionId, Instant.now());

        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void should_still_revoke_the_session() {
        when(deviceSessionRepository.findById(sessionId)).thenReturn(Optional.of(session("lab-01")));
        when(deviceSessionRepository.revokeDeviceSession(any(), any())).thenReturn(1);

        var now = Instant.now();
        provider.revoke(sessionId, now);

        verify(deviceSessionRepository).revokeDeviceSession(sessionId, now);
    }

    private DeviceSession session(String deviceId) {
        return new DeviceSession(
            sessionId, userId, deviceId, "Lab PC", SessionPlatform.WEB,
            "127.0.0.1", "junit", null);
    }
}
