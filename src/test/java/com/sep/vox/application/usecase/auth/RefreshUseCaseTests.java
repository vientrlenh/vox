package com.sep.vox.application.usecase.auth;

import com.sep.vox.application.usecase.TestUserSchoolResolver;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.application.port.input.usecase.auth.RefreshUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

class RefreshUseCaseTests {

    private DeviceSessionRepository deviceSessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private SessionManagerPort sessionManagerPort;
    private RefreshUseCase refreshUseCase;
    private SessionTokenManagerPort sessionTokenManagerPort;
    private AuthTokenPort authTokenPort;

    @BeforeEach
    void setUp() {
        deviceSessionRepository = mock(DeviceSessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        sessionManagerPort = mock(SessionManagerPort.class);
        sessionTokenManagerPort = mock(SessionTokenManagerPort.class);
        authTokenPort = mock(AuthTokenPort.class);
        refreshUseCase = new RefreshUseCase(
            deviceSessionRepository,
            refreshTokenRepository,
            userRepository,
            userRoleQueryRepository,
            sessionTokenManagerPort,
            sessionManagerPort,
            authTokenPort,
            TestUserSchoolResolver.create()
        );
    }

    @Test
    void refresh_should_rotate_token_when_request_is_valid() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var oldTokenId = UUID.randomUUID();
        var newTokenId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var deviceSession = activeSession(sessionId, userId, "device-1");
        var oldToken = activeRefreshToken(oldTokenId, sessionId, "old-token-hash");
        var savedNewToken = activeRefreshToken(newTokenId, sessionId, "new-token-hash");
        var user = activeUser(userId, schoolId);
        var roles = List.of(new UserRoleInfo(
            UUID.randomUUID(),
            userId,
            roleId,
            OffsetDateTime.now(),
            "SCHOOL_ADMIN",
            "School admin"
        ));

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
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(authTokenPort.generateJwtToken(userId.toString(), schoolId, "test@example.com", List.of("SCHOOL_ADMIN")))
            .thenReturn("access-token");

        var result = refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(sessionTokenManagerPort).hash("old-refresh-token");
        verify(refreshTokenRepository).findByTokenHashForUpdate("old-token-hash");
        verify(deviceSessionRepository).findById(sessionId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository).markUsedAndReplacedBy(any(UUID.class), any(UUID.class), any(OffsetDateTime.class));
        verify(userRepository).findById(userId);
        verify(userRoleQueryRepository).findByUserIdWithRoleInfo(userId);
        verify(authTokenPort).generateJwtToken(userId.toString(), schoolId, "test@example.com", List.of("SCHOOL_ADMIN"));
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
        return activeSession(sessionId, UUID.randomUUID(), deviceId);
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

    private static User activeUser(UUID userId, UUID schoolId) {
        TestUserSchoolResolver.remember(userId, schoolId);
        return new User(
            userId,
            new Email("test@example.com"),
            "password-hash",
            new Phone("0987654321"),
            new FullName("Test User"),
            null,
            new DateOfBirth(LocalDate.of(2000, 1, 1)),
            "Ho Chi Minh City",
            null,
            UserStatus.ACTIVE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
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
