package com.sep.vox.application.usecase.auth;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.auth.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.auth.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.OAuth2LoginUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
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
class OAuth2LoginUseCaseTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleQueryRepository userRoleQueryRepository;
    @Mock
    private DeviceSessionRepository deviceSessionRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private AuthTokenPort authTokenPort;
    @Mock
    private SessionTokenManagerPort sessionTokenManagerPort;
    @InjectMocks
    private OAuth2LoginUseCase oAuth2LoginUseCase;

    @Test
    void oauth2Login_should_return_tokens_when_google_email_matches_active_user() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = activeUser(userId, schoolId);
        SchoolUser schoolUser = activeSchoolUser(userId, schoolId);
        DeviceSession savedDeviceSession = DeviceSession.builder()
            .id(sessionId)
            .userId(userId)
            .deviceId("device-1")
            .deviceName("Chrome on Windows")
            .platform(SessionPlatform.WEB)
            .ipAddress("203.0.113.10")
            .userAgent("JUnit User Agent")
            .build();
        List<UserRoleInfo> roles = List.of(new UserRoleInfo(
            UUID.randomUUID(),
            userId,
            roleId,
            Instant.now(),
            "STUDENT",
            "Student"
        ));

        when(userRepository.findByEmailAndStatus("student@example.com", UserStatus.ACTIVE))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(schoolUserRepository.findByUserId(userId)).thenReturn(Optional.of(schoolUser));
        when(deviceSessionRepository.save(any(DeviceSession.class)))
            .thenReturn(savedDeviceSession);
        when(authTokenPort.generateJwtToken(userId.toString(), schoolId, "student@example.com", List.of("STUDENT")))
            .thenReturn("access-token");
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("refresh-token", "hashed-refresh-token"));

        LoginResponse result = oAuth2LoginUseCase.execute(validCommand());

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.roles()).containsExactly("STUDENT");

        ArgumentCaptor<DeviceSession> deviceSessionCaptor = ArgumentCaptor.forClass(DeviceSession.class);
        verify(deviceSessionRepository).save(deviceSessionCaptor.capture());
        DeviceSession deviceSession = deviceSessionCaptor.getValue();
        assertThat(deviceSession.getUserId()).isEqualTo(userId);
        assertThat(deviceSession.getDeviceId()).isEqualTo("device-1");
        assertThat(deviceSession.getDeviceName()).isEqualTo("Chrome on Windows");
        assertThat(deviceSession.getPlatform()).isEqualTo(SessionPlatform.WEB);
        assertThat(deviceSession.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(deviceSession.getUserAgent()).isEqualTo("JUnit User Agent");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(authTokenPort).generateJwtToken(userId.toString(), schoolId, "student@example.com", List.of("STUDENT"));
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

    private OAuth2LoginCommand validCommand() {
        return new OAuth2LoginCommand(
            "google",
            "google-user-id",
            "student@example.com",
            true,
            "Test Student",
            "https://example.com/avatar.png",
            "203.0.113.10",
            "JUnit User Agent",
            new ClientDeviceCommand("device-1", "Chrome on Windows", SessionPlatform.WEB)
        );
    }

    private User activeUser(UUID userId, UUID schoolId) {
        return User.builder()
            .id(userId)
            .email(new Email("student@example.com"))
            .passwordHash("password-hash")
            .phone(new Phone("0987654321"))
            .fullName(new FullName("Test Student"))
            .dateOfBirth(new DateOfBirth(LocalDate.of(2000, 1, 1)))
            .address("Ho Chi Minh City")
            .status(UserStatus.ACTIVE)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
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
