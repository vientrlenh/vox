package com.sep.vox.application.usecase.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.usecase.auth.LogoutUseCase;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;

class LogoutUseCaseTests {

    private RefreshTokenRepository refreshTokenRepository;
    private DeviceSessionRepository deviceSessionRepository;
    private SessionTokenManagerPort sessionTokenManagerPort;
    private SessionManagerPort sessionManagerPort;
    private UserContextPort userContextPort;
    private LogoutUseCase logoutUseCase;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        deviceSessionRepository = mock(DeviceSessionRepository.class);
        sessionTokenManagerPort = mock(SessionTokenManagerPort.class);
        sessionManagerPort = mock(SessionManagerPort.class);
        userContextPort = mock(UserContextPort.class);
        logoutUseCase = new LogoutUseCase(
            refreshTokenRepository,
            deviceSessionRepository,
            sessionTokenManagerPort,
            sessionManagerPort,
            userContextPort
        );
    }

    @Test
    void logout_should_revoke_session_of_presented_refresh_token() {
        var sessionId = UUID.randomUUID();
        when(sessionTokenManagerPort.hash("refresh-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
            .thenReturn(Optional.of(activeRefreshToken(sessionId, "token-hash")));
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        logoutUseCase.execute(new LogoutCommand("refresh-token", "device-1"));

        verify(sessionManagerPort).revoke(eq(sessionId), any(Instant.class));
    }

    /**
     * Đúng trạng thái của một phiên bị bỏ quên: access token đã hết hạn nên không xác định được
     * người gọi, chỉ còn cookie làm bằng chứng. Hỏng ở đây là phiên không bao giờ thu hồi được.
     */
    @Test
    void logout_should_revoke_by_cookie_when_caller_is_not_authenticated() {
        var sessionId = UUID.randomUUID();
        when(sessionTokenManagerPort.hash("refresh-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
            .thenReturn(Optional.of(activeRefreshToken(sessionId, "token-hash")));
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        logoutUseCase.execute(new LogoutCommand("refresh-token", "device-1"));

        verify(sessionManagerPort).revoke(eq(sessionId), any(Instant.class));
        verifyNoInteractions(deviceSessionRepository);
    }

    /**
     * LoginUseCase tạo DeviceSession mới ở mỗi lần đăng nhập, nên cùng một máy có thể đang mang
     * nhiều phiên sống. Cookie chỉ trỏ tới một cái; phần còn lại phải được dọn theo.
     */
    @Test
    void logout_should_revoke_every_live_session_on_the_same_device() {
        var userId = UUID.randomUUID();
        var cookieSessionId = UUID.randomUUID();
        var strandedSessionId = UUID.randomUUID();

        when(sessionTokenManagerPort.hash("refresh-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
            .thenReturn(Optional.of(activeRefreshToken(cookieSessionId, "token-hash")));
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(userId));
        when(deviceSessionRepository.findByUserId(userId)).thenReturn(List.of(
            activeSession(cookieSessionId, userId, "device-1"),
            activeSession(strandedSessionId, userId, "device-1")
        ));

        logoutUseCase.execute(new LogoutCommand("refresh-token", "device-1"));

        verify(sessionManagerPort).revoke(eq(cookieSessionId), any(Instant.class));
        verify(sessionManagerPort).revoke(eq(strandedSessionId), any(Instant.class));
    }

    @Test
    void logout_should_revoke_session_once_when_both_sources_point_to_it() {
        var userId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        when(sessionTokenManagerPort.hash("refresh-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
            .thenReturn(Optional.of(activeRefreshToken(sessionId, "token-hash")));
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(userId));
        when(deviceSessionRepository.findByUserId(userId))
            .thenReturn(List.of(activeSession(sessionId, userId, "device-1")));

        logoutUseCase.execute(new LogoutCommand("refresh-token", "device-1"));

        verify(sessionManagerPort, times(1)).revoke(eq(sessionId), any(Instant.class));
    }

    @Test
    void logout_should_leave_sessions_of_other_devices_alone() {
        var userId = UUID.randomUUID();
        var otherDeviceSessionId = UUID.randomUUID();

        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(userId));
        when(deviceSessionRepository.findByUserId(userId))
            .thenReturn(List.of(activeSession(otherDeviceSessionId, userId, "device-2")));

        logoutUseCase.execute(new LogoutCommand(null, "device-1"));

        verify(sessionManagerPort, never()).revoke(any(UUID.class), any(Instant.class));
    }

    @Test
    void logout_should_skip_sessions_that_are_already_revoked() {
        var userId = UUID.randomUUID();
        var revokedSessionId = UUID.randomUUID();

        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.of(userId));
        when(deviceSessionRepository.findByUserId(userId))
            .thenReturn(List.of(revokedSession(revokedSessionId, userId, "device-1")));

        logoutUseCase.execute(new LogoutCommand(null, "device-1"));

        verify(sessionManagerPort, never()).revoke(any(UUID.class), any(Instant.class));
    }

    /**
     * Cookie cũ hoặc cookie của môi trường khác không tra ra token nào. Đăng xuất vẫn phải thành
     * công: client xoá token trong máy bất kể server trả gì.
     */
    @Test
    void logout_should_not_fail_when_refresh_token_is_unknown() {
        when(sessionTokenManagerPort.hash("stale-token")).thenReturn("stale-hash");
        when(refreshTokenRepository.findByTokenHash("stale-hash")).thenReturn(Optional.empty());
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        logoutUseCase.execute(new LogoutCommand("stale-token", "device-1"));

        verify(sessionManagerPort, never()).revoke(any(UUID.class), any(Instant.class));
    }

    @Test
    void logout_should_not_hash_anything_when_cookie_is_missing() {
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        logoutUseCase.execute(new LogoutCommand(null, "device-1"));

        verifyNoInteractions(sessionTokenManagerPort);
        verifyNoInteractions(refreshTokenRepository);
        verify(sessionManagerPort, never()).revoke(any(UUID.class), any(Instant.class));
    }

    private static DeviceSession activeSession(UUID sessionId, UUID userId, String deviceId) {
        return new DeviceSession(
            sessionId,
            userId,
            deviceId,
            "Chrome on Windows",
            SessionPlatform.WEB,
            "203.0.113.10",
            "JUnit User Agent",
            null
        );
    }

    private static DeviceSession revokedSession(UUID sessionId, UUID userId, String deviceId) {
        return new DeviceSession(
            sessionId,
            userId,
            deviceId,
            "Chrome on Windows",
            SessionPlatform.WEB,
            "203.0.113.10",
            "JUnit User Agent",
            Instant.now()
        );
    }

    private static RefreshToken activeRefreshToken(UUID sessionId, String tokenHash) {
        var now = Instant.now();
        return new RefreshToken(
            UUID.randomUUID(),
            sessionId,
            tokenHash,
            now,
            now.plus(7, ChronoUnit.DAYS),
            null,
            null
        );
    }
}
