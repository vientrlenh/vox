package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.OAuth2LoginUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
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

class OAuth2LoginUseCaseTests {

    private UserRepository userRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private DeviceSessionRepository deviceSessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AuthTokenPort authTokenPort;
    private SessionTokenManagerPort sessionTokenManagerPort;
    private OAuth2LoginUseCase oAuth2LoginUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        deviceSessionRepository = mock(DeviceSessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        authTokenPort = mock(AuthTokenPort.class);
        sessionTokenManagerPort = mock(SessionTokenManagerPort.class);
        oAuth2LoginUseCase = new OAuth2LoginUseCase(
            userRepository,
            userRoleQueryRepository,
            deviceSessionRepository,
            refreshTokenRepository,
            authTokenPort,
            sessionTokenManagerPort
        );
    }

    @Test
    void oauth2Login_should_return_tokens_when_google_email_matches_active_user() {
        var userId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var user = activeUser(userId);
        var savedDeviceSession = new DeviceSession(
            sessionId,
            userId,
            "device-1",
            "Chrome on Windows",
            SessionPlatform.WEB,
            "203.0.113.10",
            "JUnit User Agent",
            null
        );
        var roles = List.of(new UserRoleInfo(
            UUID.randomUUID(),
            userId,
            roleId,
            OffsetDateTime.now(),
            "STUDENT",
            "Student"
        ));

        when(userRepository.findByEmailAndStatus("student@example.com", UserStatus.ACTIVE))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(deviceSessionRepository.save(any(DeviceSession.class)))
            .thenReturn(savedDeviceSession);
        when(authTokenPort.generateJwtToken(userId.toString(), "student@example.com", List.of("STUDENT")))
            .thenReturn("access-token");
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("refresh-token", "hashed-refresh-token"));

        var result = oAuth2LoginUseCase.execute(validCommand());

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.roles()).containsExactly("STUDENT");

        var deviceSessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);
        verify(deviceSessionRepository).save(deviceSessionCaptor.capture());
        var deviceSession = deviceSessionCaptor.getValue();
        assertThat(deviceSession.getUserId()).isEqualTo(userId);
        assertThat(deviceSession.getDeviceId()).isEqualTo("device-1");
        assertThat(deviceSession.getDeviceName()).isEqualTo("Chrome on Windows");
        assertThat(deviceSession.getPlatform()).isEqualTo(SessionPlatform.WEB);
        assertThat(deviceSession.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(deviceSession.getUserAgent()).isEqualTo("JUnit User Agent");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(authTokenPort).generateJwtToken(userId.toString(), "student@example.com", List.of("STUDENT"));
    }

    @Test
    void oauth2Login_should_throw_unauthorized_when_email_is_not_verified() {
        assertThrows(
            UnauthorizedException.class,
            () -> oAuth2LoginUseCase.execute(new OAuth2LoginCommand(
                "google",
                "google-user-id",
                "student@example.com",
                false,
                "Test Student",
                "https://example.com/avatar.png",
                "203.0.113.10",
                "JUnit User Agent",
                new ClientDeviceCommand("device-1", "Chrome on Windows", "WEB")
            ))
        );

        verifyNoInteractions(
            userRepository,
            userRoleQueryRepository,
            deviceSessionRepository,
            refreshTokenRepository,
            authTokenPort,
            sessionTokenManagerPort
        );
    }

    @Test
    void oauth2Login_should_throw_unauthorized_when_active_user_is_not_found() {
        when(userRepository.findByEmailAndStatus("student@example.com", UserStatus.ACTIVE))
            .thenReturn(Optional.empty());

        assertThrows(
            UnauthorizedException.class,
            () -> oAuth2LoginUseCase.execute(validCommand())
        );

        verify(userRepository).findByEmailAndStatus("student@example.com", UserStatus.ACTIVE);
        verifyNoInteractions(
            userRoleQueryRepository,
            deviceSessionRepository,
            refreshTokenRepository,
            authTokenPort,
            sessionTokenManagerPort
        );
    }

    private static OAuth2LoginCommand validCommand() {
        return new OAuth2LoginCommand(
            "google",
            "google-user-id",
            "student@example.com",
            true,
            "Test Student",
            "https://example.com/avatar.png",
            "203.0.113.10",
            "JUnit User Agent",
            new ClientDeviceCommand("device-1", "Chrome on Windows", "WEB")
        );
    }

    private static User activeUser(UUID userId) {
        return new User(
            userId,
            new Email("student@example.com"),
            "password-hash",
            new Phone("0987654321"),
            new FullName("Test Student"),
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
}
