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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
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
public class LoginUseCaseTests {

    @Mock
    private AuthenticationManagerPort authenticationManagerPort;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleQueryRepository userRoleQueryRepository;
    @Mock
    private SchoolUserRepository schoolUserRepository;
    @Mock
    private AuthTokenPort authTokenPort;
    @Mock
    private SessionTokenManagerPort sessionTokenManagerPort;
    @Mock
    private DeviceSessionRepository deviceSessionRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks
    private LoginUseCase loginUseCase;

    @Test
    void login_should_return_tokens_when_credentials_are_valid() {
        UUID userId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = activeUser(userId, schoolId, "test@example.com");
        SchoolUser schoolUser = activeSchoolUser(userId, schoolId);
        DeviceSession deviceSession = DeviceSession.builder()
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
            "SCHOOL_ADMIN",
            "School admin"
        ));
        
        when(authenticationManagerPort.setAuthenticationAndGetUserId("test@example.com", "123456"))
            .thenReturn(userId);
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(schoolUserRepository.findByUserId(userId))
            .thenReturn(Optional.of(schoolUser));
        when(authTokenPort.generateJwtToken(userId.toString(), schoolId, user.getEmail().value(), List.of("SCHOOL_ADMIN")))
            .thenReturn("access-token");
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("refresh-token", "hashed-refresh-token"));
        when(deviceSessionRepository.save(any(DeviceSession.class)))
            .thenReturn(deviceSession);

        LoginResponse result = loginUseCase.execute(validLoginCommand("test@example.com", "123456"));
        
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        verify(authenticationManagerPort).setAuthenticationAndGetUserId("test@example.com", "123456");
        verify(userRepository).findById(userId);
        verify(userRoleQueryRepository).findByUserIdWithRoleInfo(userId);
        verify(authTokenPort).generateJwtToken(userId.toString(), schoolId, user.getEmail().value(), List.of("SCHOOL_ADMIN"));
        verify(sessionTokenManagerPort).generateToken();
        verify(deviceSessionRepository).save(any(DeviceSession.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_should_generate_token_when_user_has_no_school() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = activeUser(userId, null, "sysadmin@example.com");
        DeviceSession deviceSession = DeviceSession.builder()
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
            "SYSTEM_ADMIN",
            "System admin"
        ));

        when(authenticationManagerPort.setAuthenticationAndGetUserId("sysadmin@example.com", "123456"))
            .thenReturn(userId);
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(authTokenPort.generateJwtToken(userId.toString(), null, user.getEmail().value(), List.of("SYSTEM_ADMIN")))
            .thenReturn("access-token");
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("refresh-token", "hashed-refresh-token"));
        when(deviceSessionRepository.save(any(DeviceSession.class)))
            .thenReturn(deviceSession);

        LoginResponse result = loginUseCase.execute(validLoginCommand("sysadmin@example.com", "123456"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(authTokenPort).generateJwtToken(userId.toString(), null, user.getEmail().value(), List.of("SYSTEM_ADMIN"));
    }

    @Test
    void login_should_reject_invalid_credentials() {
        when(authenticationManagerPort.setAuthenticationAndGetUserId("test@example.com", "wrong-password"))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
            AuthenticationException.class, 
            () -> loginUseCase.execute(validLoginCommand("test@example.com", "wrong-password"))
        );

        verify(authenticationManagerPort).setAuthenticationAndGetUserId("test@example.com", "wrong-password");
        verifyNoInteractions(
            userRepository,
            userRoleQueryRepository,
            authTokenPort,
            sessionTokenManagerPort,
            deviceSessionRepository,
            refreshTokenRepository
        );
    }

    @Test
    void login_should_throw_not_found_when_authenticated_user_does_not_exist() {
        UUID userId = UUID.randomUUID();
        when(authenticationManagerPort.setAuthenticationAndGetUserId("missing@example.com", "123456"))
            .thenReturn(userId);
        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());
        
        assertThrows(
            NotFoundException.class,
            () -> loginUseCase.execute(validLoginCommand("missing@example.com", "123456"))
        );
        
        verify(authenticationManagerPort).setAuthenticationAndGetUserId("missing@example.com", "123456");
        verify(userRepository).findById(userId);
        verifyNoInteractions(
            userRoleQueryRepository,
            authTokenPort,
            sessionTokenManagerPort,
            deviceSessionRepository,
            refreshTokenRepository
        );
    }

    private LoginCommand validLoginCommand(String login, String password) {
        return new LoginCommand(
            login,
            password,
            "203.0.113.10",
            "JUnit User Agent",
            new ClientDeviceCommand("device-1", "Chrome on Windows", SessionPlatform.WEB)
        );
    }

    private User activeUser(UUID userId, UUID schoolId, String email) {
        return User.builder()
            .id(userId)
            .email(new Email(email))
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

    private SchoolUser activeSchoolUser(UUID userId, UUID schoolId) {
        return SchoolUser.builder()
            .schoolId(schoolId)
            .userId(userId)
            .startDate(Instant.now())
            .endDate(Instant.now())
            .build();
    }
}
