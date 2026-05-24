package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;

public class LoginUseCaseTests {

    private AuthenticationManagerPort authenticationManagerPort;
    private UserRepository userRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private AuthTokenPort authTokenPort;
    private SessionManagerPort sessionManagerPort;
    private LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        authenticationManagerPort = mock(AuthenticationManagerPort.class);
        userRepository = mock(UserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);
        authTokenPort = mock(AuthTokenPort.class);
        sessionManagerPort = mock(SessionManagerPort.class);
        loginUseCase = new LoginUseCase(
            authenticationManagerPort,
            userRepository,
            userRoleQueryRepository,
            authTokenPort,
            sessionManagerPort
        );
    }

    @Test
    void login_should_return_tokens_when_credentials_are_valid() {
        var userId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var user = activeUser(userId);
        var roles = List.of(new UserRoleInfo(
            1L,
            userId,
            roleId,
            OffsetDateTime.now(),
            "SCHOOL_ADMIN",
            "School admin"
        ));
        
        when(authenticationManagerPort.setAuthenticationAndGetUserEmail("test@example.com", "123456"))
            .thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(user));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(userId))
            .thenReturn(roles);
        when(authTokenPort.generateJwtToken(userId.toString(), List.of("SCHOOL_ADMIN")))
            .thenReturn("access-token");
        when(sessionManagerPort.setSessionAndGetRefreshTokenWhenLogin(userId))
            .thenReturn("refresh-token");

        var result = loginUseCase.execute(new LoginCommand("test@example.com", "123456"));
        
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        verify(authenticationManagerPort).setAuthenticationAndGetUserEmail("test@example.com", "123456");
        verify(userRepository).findByEmail("test@example.com");
        verify(userRoleQueryRepository).findByUserIdWithRoleInfo(userId);
        verify(authTokenPort).generateJwtToken(userId.toString(), List.of("SCHOOL_ADMIN"));
        verify(sessionManagerPort).setSessionAndGetRefreshTokenWhenLogin(userId);
    }

    @Test
    void login_should_reject_invalid_credentials() {
        when(authenticationManagerPort.setAuthenticationAndGetUserEmail("test@example.com", "wrong-password"))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
            AuthenticationException.class, 
            () -> loginUseCase.execute(new LoginCommand("test@example.com", "wrong-password"))
        );

        verify(authenticationManagerPort).setAuthenticationAndGetUserEmail("test@example.com", "wrong-password");
        verifyNoInteractions(userRepository, userRoleQueryRepository, authTokenPort, sessionManagerPort);
    }

    @Test
    void login_should_throw_not_found_when_authenticated_user_does_not_exist() {
        when(authenticationManagerPort.setAuthenticationAndGetUserEmail("missing@example.com", "123456"))
            .thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com"))
            .thenReturn(Optional.empty());
        
        assertThrows(
            NotFoundException.class,
            () -> loginUseCase.execute(new LoginCommand("missing@example.com", "123456"))
        );
        
        verify(authenticationManagerPort).setAuthenticationAndGetUserEmail("missing@example.com", "123456");
        verify(userRepository).findByEmail("missing@example.com");
        verifyNoInteractions(userRoleQueryRepository, authTokenPort, sessionManagerPort);
    }

    private User activeUser(UUID userId) {
        return new User(
            userId,
            new Email("test@example.com"),
            "password-hash",
            new Phone("0987654321"),
            "Test User",
            null,
            LocalDate.of(2000, 1, 1),
            "Ho Chi Minh City",
            UserStatus.ACTIVE,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            null,
            null
        );
    }
}
