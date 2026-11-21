package com.sep.vox.application.usecase.auth;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

@ExtendWith(MockitoExtension.class)
class RefreshUseCaseTests {

    @Mock
    private DeviceSessionRepository deviceSessionRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleQueryRepository userRoleQueryRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private SessionManagerPort sessionManagerPort;
    @Mock
    private SessionTokenManagerPort sessionTokenManagerPort;
    @Mock
    private AuthTokenPort authTokenPort;
    @InjectMocks
    private RefreshUseCase refreshUseCase;

    @Test
    void refresh_should_rotate_token_when_request_is_valid() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID oldTokenId = UUID.randomUUID();
        UUID newTokenId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        DeviceSession deviceSession = activeSession(sessionId, userId, "device-1");
        RefreshToken oldToken = activeRefreshToken(oldTokenId, sessionId, "old-token-hash");
        RefreshToken savedNewToken = activeRefreshToken(newTokenId, sessionId, "new-token-hash");
        User user = activeUser(userId, schoolId);
        SchoolUser schoolUser = activeSchoolUser(userId, schoolId);
        List<UserRoleInfo> roles = List.of(new UserRoleInfo(
            UUID.randomUUID(),
            userId,
            roleId,
            Instant.now(),
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
        when(refreshTokenRepository.markUsedAndReplacedBy(any(UUID.class), any(UUID.class), any(Instant.class)))
            .thenReturn(1);
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        when(authTokenPort.generateJwtToken(userId.toString(), schoolId, "test@example.com", List.of("SCHOOL_ADMIN")))
            .thenReturn("access-token");

        RefreshResponse result = refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(sessionTokenManagerPort).hash("old-refresh-token");
        verify(refreshTokenRepository).findByTokenHashForUpdate("old-token-hash");
        verify(deviceSessionRepository).findById(sessionId);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(refreshTokenRepository).markUsedAndReplacedBy(any(UUID.class), any(UUID.class), any(Instant.class));
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
        UUID sessionId = UUID.randomUUID();
        RefreshToken oldToken = activeRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
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
        UUID sessionId = UUID.randomUUID();
        RefreshToken oldToken = expiredRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
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
        UUID sessionId = UUID.randomUUID();
        RefreshToken oldToken = usedRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(activeSession(sessionId, "device-1")));

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"))
        );

        verify(sessionManagerPort).revoke(eq(sessionId), any(Instant.class));
    }

    @Test
    void refresh_should_revoke_session_and_throw_unauthorized_when_device_id_mismatches() {
        UUID sessionId = UUID.randomUUID();
        RefreshToken oldToken = activeRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(activeSession(sessionId, "device-1")));

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "different-device"))
        );

        verify(sessionManagerPort).revoke(eq(sessionId), any(Instant.class));
    }

    @Test
    void refresh_should_throw_unauthorized_when_device_session_is_revoked() {
        UUID sessionId = UUID.randomUUID();
        RefreshToken oldToken = activeRefreshToken(UUID.randomUUID(), sessionId, "old-token-hash");
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
        UUID sessionId = UUID.randomUUID();
        UUID oldTokenId = UUID.randomUUID();
        UUID newTokenId = UUID.randomUUID();
        RefreshToken oldToken = activeRefreshToken(oldTokenId, sessionId, "old-token-hash");
        RefreshToken savedNewToken = activeRefreshToken(newTokenId, sessionId, "new-token-hash");

        when(sessionTokenManagerPort.hash("old-refresh-token")).thenReturn("old-token-hash");
        when(refreshTokenRepository.findByTokenHashForUpdate("old-token-hash"))
            .thenReturn(Optional.of(oldToken));
        when(deviceSessionRepository.findById(sessionId))
            .thenReturn(Optional.of(activeSession(sessionId, "device-1")));
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("new-refresh-token", "new-token-hash"));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
            .thenReturn(savedNewToken);
        when(refreshTokenRepository.markUsedAndReplacedBy(any(UUID.class), any(UUID.class), any(Instant.class)))
            .thenReturn(0);

        assertThrows(
            UnauthorizedException.class,
            () -> refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1"))
        );

        verify(sessionManagerPort).revoke(eq(sessionId), any(Instant.class));
    }

    private static DeviceSession activeSession(UUID sessionId, String deviceId) {
        return activeSession(sessionId, UUID.randomUUID(), deviceId);
    }

    private static DeviceSession activeSession(UUID sessionId, UUID userId, String deviceId) {
        return DeviceSession.builder()
            .id(sessionId)
            .userId(userId)
            .deviceId(deviceId)
            .deviceName("Chrome on Windows")
            .platform(SessionPlatform.WEB)
            .ipAddress("203.0.113.10")
            .userAgent("JUnit User Agent")
            .build();
    }

    private User activeUser(UUID userId, UUID schoolId) {
        return User.builder()
            .id(userId)
            .email(new Email("test@example.com"))
            .passwordHash("password-hash")
            .phone(new Phone("0987654321"))
            .fullName(new FullName("Test User"))
            .dateOfBirth(new DateOfBirth(LocalDate.of(2000, 1, 1)))
            .address("Ho Chi Minh City")
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    private DeviceSession revokedSession(UUID sessionId, String deviceId) {
        return DeviceSession.builder()
            .id(sessionId)
            .userId(UUID.randomUUID())
            .deviceId(deviceId)
            .deviceName("Chrome on Windows")
            .platform(SessionPlatform.WEB)
            .ipAddress("203.0.113.10")
            .userAgent("JUnit User Agent")
            .revokedAt(Instant.now())
            .build();
    }

    private RefreshToken activeRefreshToken(UUID tokenId, UUID sessionId, String tokenHash) {
        Instant now = Instant.now();
        return RefreshToken.builder()
            .id(tokenId)
            .sessionId(sessionId)
            .tokenHash(tokenHash)
            .issuedAt(now)
            .expiredAt(now.plus(7, ChronoUnit.DAYS))
            .build();
    }

    private RefreshToken expiredRefreshToken(UUID tokenId, UUID sessionId, String tokenHash) {
        Instant now = Instant.now();
        return RefreshToken.builder()
            .id(tokenId)
            .sessionId(sessionId)
            .tokenHash(tokenHash)
            .issuedAt(now.minus(8, ChronoUnit.DAYS))
            .expiredAt(now.minus(1, ChronoUnit.DAYS))
            .build();
    }

    private RefreshToken usedRefreshToken(UUID tokenId, UUID sessionId, String tokenHash) {
        Instant now = Instant.now();
        return RefreshToken.builder()
            .id(tokenId)
            .sessionId(sessionId)
            .tokenHash(tokenHash)
            .issuedAt(now.minus(1, ChronoUnit.DAYS))
            .expiredAt(now.plus(6, ChronoUnit.DAYS))
            .usedAt(now)
            .replacedBy(UUID.randomUUID())
            .build();
    }

    private SchoolUser activeSchoolUser(UUID userId, UUID schoolId) {
        return SchoolUser.builder()
            .schoolId(schoolId)
            .userId(userId)
            .startDate(Instant.now())
            .endDate(Instant.now())
            .build();
    }
}
