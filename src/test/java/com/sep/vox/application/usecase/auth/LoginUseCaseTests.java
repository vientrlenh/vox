package com.sep.vox.application.usecase.auth;

import com.sep.vox.application.usecase.TestSchoolUserRepository;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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

public class LoginUseCaseTests {

    private AuthenticationManagerPort authenticationManagerPort;
    private UserRepository userRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private AuthTokenPort authTokenPort;
    private SessionTokenManagerPort sessionTokenManagerPort;
    private DeviceSessionRepository deviceSessionRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        authenticationManagerPort = mock(AuthenticationManagerPort.class);
        userRepository = mock(UserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        authTokenPort = mock(AuthTokenPort.class);
        sessionTokenManagerPort = mock(SessionTokenManagerPort.class);
        deviceSessionRepository = mock(DeviceSessionRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        var schoolUserRepository = TestSchoolUserRepository.create();
        loginUseCase = new LoginUseCase(
            authenticationManagerPort,
            userRepository,
            userRoleQueryRepository,
            authTokenPort,
            sessionTokenManagerPort,
            deviceSessionRepository,
            refreshTokenRepository,
            schoolUserRepository
        );
    }

    @Test
    void login_should_return_tokens_when_credentials_are_valid() {
        var userId = UUID.randomUUID();
        var schoolId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var user = activeUser(userId, schoolId, "test@example.com");
        var deviceSession = new DeviceSession(
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
            "SCHOOL_ADMIN",
            "School admin"
        ));
        
        when(authenticationManagerPort.setAuthenticationAndGetUserId("test@example.com", "123456"))
            .thenReturn(userId);
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(authTokenPort.generateJwtToken(userId.toString(), schoolId, user.getEmail().value(), List.of("SCHOOL_ADMIN")))
            .thenReturn("access-token");
        when(sessionTokenManagerPort.generateToken())
            .thenReturn(new GeneratedSessionToken("refresh-token", "hashed-refresh-token"));
        when(deviceSessionRepository.save(any(DeviceSession.class)))
            .thenReturn(deviceSession);

        var result = loginUseCase.execute(validLoginCommand("test@example.com", "123456"));
        
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
        var userId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var user = activeUser(userId, null, "sysadmin@example.com");
        var deviceSession = new DeviceSession(
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

        var result = loginUseCase.execute(validLoginCommand("sysadmin@example.com", "123456"));

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
        var userId = UUID.randomUUID();
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
            new ClientDeviceCommand("device-1", "Chrome on Windows", "WEB", null)
        );
    }

    private User activeUser(UUID userId, UUID schoolId, String email) {
        TestSchoolUserRepository.remember(userId, schoolId);
        return new User(
            userId,
            new Email(email),
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
}
