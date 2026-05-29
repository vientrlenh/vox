package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.application.port.input.usecase.auth.RefreshUseCase;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;

class RefreshUseCaseTests {

    private DeviceSessionRepository deviceSessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private SessionManagerPort sessionManagerPort;
    private RefreshUseCase refreshUseCase;
    private SessionTokenManagerPort sessionTokenManagerPort;

    @BeforeEach
    void setUp() {
        deviceSessionRepository = mock(DeviceSessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        sessionManagerPort = mock(SessionManagerPort.class);
        sessionTokenManagerPort = mock(SessionTokenManagerPort.class);
        refreshUseCase = new RefreshUseCase(deviceSessionRepository, refreshTokenRepository, sessionTokenManagerPort, sessionManagerPort);
    }

    @Test
    void refresh_should_rotate_token_when_request_is_valid() {
        var sessionId = UUID.randomUUID();
        var oldTokenId = UUID.randomUUID();
        var newTokenId = UUID.randomUUID();
        var deviceSession = activeSession(sessionId, "device-1");
        var oldToken = activeRefreshToken(oldTokenId, sessionId, "old-token-hash");
        var savedNewToken = activeRefreshToken(newTokenId, sessionId, "new-token-hash");

        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(deviceSession));
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("new-refresh-token", "new-token-hash"));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
            .thenReturn(savedNewToken);
        when(refreshTokenRepository.markUsedAndReplacedBy(any(UUID.class), any(UUID.class), any(OffsetDateTime.class)))
            .thenReturn(1);

        var result = refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"));

        assertThat(result.token()).isEqualTo("new-refresh-token");
        verify(sessionTokenManagerPort).hash("old-refresh-token");
        verify(refreshTokenRepository).findByTokenHashForUpdate("old-token-hash");
        verify(deviceSessionRepository).findById(sessionId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository).markUsedAndReplacedBy(any(UUID.class), any(UUID.class), any(OffsetDateTime.class));
    }

    @Test
    void refresh_should_throw_unauthorized_when_token_not_found() {
        when(sessionTokenManagerPort.hash("missing-token")).thenReturn("missing-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("missing-token-hash"))
            .thenReturn(Optional.empty());

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("missing-token", "device-1"))
        );

        verifyNoInteractions(deviceSessionRepository);
    }

    @Test
    void refresh_should_throw_unauthorized_when_device_session_not_found() {
        var sessionId = UUID.randomUUID();
        var oldToken = activeRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.empty());

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"))
        );

        verify(sessionTokenManagerPort, never()).generateToken();
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void refresh_should_throw_unauthorized_when_token_is_expired() {
        var sessionId = UUID.randomUUID();
        var oldToken = expiredRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(activeSession(sessionId, "device-1")));

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"))
        );

        verify(refreshTokenRepository).findByTokenHashForUpdate("old-token-hash");
    }

    @Test
    void refresh_should_revoke_session_and_throw_unauthorized_when_token_is_used() {
        var sessionId = UUID.randomUUID();
        var oldToken = usedRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(activeSession(sessionId, "device-1")));

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"))
        );

        verify(sessionManagerPort).revoke(eq(sessionId), any(OffsetDateTime.class));
    }

    @Test
    void refresh_should_revoke_session_and_throw_unauthorized_when_device_id_mismatches() {
        var sessionId = UUID.randomUUID();
        var oldToken = activeRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(activeSession(sessionId, "device-1")));

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "different-device"))
        );

        verify(sessionManagerPort).revoke(eq(sessionId), any(OffsetDateTime.class));
    }

    @Test
    void refresh_should_throw_unauthorized_when_device_session_is_revoked() {
        var sessionId = UUID.randomUUID();
        var oldToken = activeRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(revokedSession(sessionId, "device-1")));

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"))
        );
    }

    @Test
    void refresh_should_revoke_session_and_throw_unauthorized_when_old_token_update_fails() {
        var sessionId = UUID.randomUUID();
        var oldTokenId = UUID.randomUUID();
        var newTokenId = UUID.randomUUID();
        var oldToken = activeRefreshToken(oldTokenId, sessionId, "old-token-hash");
        var savedNewToken = activeRefreshToken(newTokenId, sessionId, "new-token-hash");

        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(activeSession(sessionId, "device-1")));
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("new-refresh-token", "new-token-hash"));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
            .thenReturn(savedNewToken);
        when(refreshTokenRepository.markUsedAndReplacedBy(any(UUID.class), any(UUID.class), any(OffsetDateTime.class)))
            .thenReturn(0);

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"))
        );

        verify(sessionManagerPort).revoke(eq(sessionId), any(OffsetDateTime.class));
    }

    private static DeviceSession activeSession(UUID sessionId, String deviceId) {
        return new DeviceSession(
            sessionId,
            UUID.randomUUID(),
            deviceId,
            "Chrome on Windows",
            SessionPlatform.WEB,
            "203.0.113.10",
            "JUnit User Agent",
            null
        );
    }

    private static DeviceSession revokedSession(UUID sessionId, String deviceId) {
        return new DeviceSession(
            sessionId,
            UUID.randomUUID(),
            deviceId,
            "Chrome on Windows",
            SessionPlatform.WEB,
            "203.0.113.10",
            "JUnit User Agent",
            OffsetDateTime.now()
        );
    }

    private static RefreshToken activeRefreshToken(UUID tokenId, UUID sessionId, String tokenHash) {
        var now = OffsetDateTime.now();
        return new RefreshToken(
            tokenId,
            sessionId,
            tokenHash,
            now,
            now.plusDays(7),
            null,
            null
        );
    }

    private static RefreshToken expiredRefreshToken(UUID tokenId, UUID sessionId, String tokenHash) {
        var now = OffsetDateTime.now();
        return new RefreshToken(
            tokenId,
            sessionId,
            tokenHash,
            now.minusDays(8),
            now.minusDays(1),
            null,
            null
        );
    }

    private static RefreshToken usedRefreshToken(UUID tokenId, UUID sessionId, String tokenHash) {
        var now = OffsetDateTime.now();
        return new RefreshToken(
            tokenId,
            sessionId,
            tokenHash,
            now.minusDays(1),
            now.plusDays(6),
            now,
            UUID.randomUUID()
        );
    }
}
