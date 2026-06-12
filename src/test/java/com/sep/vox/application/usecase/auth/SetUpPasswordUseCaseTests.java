package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SetUpPasswordCommand;
import com.sep.vox.application.port.input.usecase.auth.SetUpPasswordUseCase;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

public class SetUpPasswordUseCaseTests {

    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private PasswordSetUpTokenPort passwordSetUpTokenPort;
    private UserRepository userRepository;
    private PasswordEncoderPort passwordEncoderPort;
    private SetUpPasswordUseCase setUpPasswordUseCase;

    @BeforeEach
    void setUp() {
        passwordSetUpTokenRepository = mock(PasswordSetUpTokenRepository.class);
        passwordSetUpTokenPort = mock(PasswordSetUpTokenPort.class);
        userRepository = mock(UserRepository.class);
        passwordEncoderPort = mock(PasswordEncoderPort.class);
        setUpPasswordUseCase = new SetUpPasswordUseCase(
            passwordSetUpTokenRepository,
            passwordSetUpTokenPort,
            userRepository,
            passwordEncoderPort
        );
    }

    @Test
    void set_up_password_should_consume_token_update_password_and_activate_user() {
        var userId = UUID.randomUUID();
        var command = new SetUpPasswordCommand(userId, "raw-token", "new-password");
        var user = inactiveUser(userId);

        when(passwordSetUpTokenPort.hash("raw-token")).thenReturn("hashed-token");
        when(passwordSetUpTokenRepository.updateUsedToken(eq(userId), eq("hashed-token"), any(OffsetDateTime.class)))
            .thenReturn(1);
        when(passwordEncoderPort.hash("new-password")).thenReturn("hashed-password");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = setUpPasswordUseCase.execute(command);

        assertThat(result).isNull();
        verify(passwordSetUpTokenPort).hash("raw-token");

        var tokenUsedAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(passwordSetUpTokenRepository).updateUsedToken(
            eq(userId),
            eq("hashed-token"),
            tokenUsedAtCaptor.capture()
        );
        assertThat(tokenUsedAtCaptor.getValue()).isNotNull();

        verify(passwordEncoderPort).hash("new-password");
        verify(userRepository).findById(userId);

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getUpdatedAt()).isEqualTo(tokenUsedAtCaptor.getValue());
    }

    @Test
    void set_up_password_should_reject_when_token_is_invalid_expired_or_used() {
        var userId = UUID.randomUUID();
        var command = new SetUpPasswordCommand(userId, "raw-token", "new-password");

        when(passwordSetUpTokenPort.hash("raw-token")).thenReturn("hashed-token");
        when(passwordSetUpTokenRepository.updateUsedToken(eq(userId), eq("hashed-token"), any(OffsetDateTime.class)))
            .thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> setUpPasswordUseCase.execute(command));

        verify(passwordSetUpTokenPort).hash("raw-token");
        verify(passwordSetUpTokenRepository).updateUsedToken(
            eq(userId),
            eq("hashed-token"),
            any(OffsetDateTime.class)
        );
        verifyNoInteractions(passwordEncoderPort, userRepository);
    }

    @Test
    void set_up_password_should_throw_when_user_is_not_found_after_token_is_consumed() {
        var userId = UUID.randomUUID();
        var command = new SetUpPasswordCommand(userId, "raw-token", "new-password");

        when(passwordSetUpTokenPort.hash("raw-token")).thenReturn("hashed-token");
        when(passwordSetUpTokenRepository.updateUsedToken(eq(userId), eq("hashed-token"), any(OffsetDateTime.class)))
            .thenReturn(1);
        when(passwordEncoderPort.hash("new-password")).thenReturn("hashed-password");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> setUpPasswordUseCase.execute(command));

        verify(passwordSetUpTokenPort).hash("raw-token");
        verify(passwordSetUpTokenRepository).updateUsedToken(
            eq(userId),
            eq("hashed-token"),
            any(OffsetDateTime.class)
        );
        verify(passwordEncoderPort).hash("new-password");
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    private User inactiveUser(UUID userId) {
        var now = OffsetDateTime.now();
        return new User(
            userId,
            new Email("admin@example.com"),
            "__PASSWORD_NOT_SET__",
            new Phone("0987654321"),
            new FullName("Nguyen Van A"),
            null,
            new DateOfBirth(LocalDate.of(2000, 5, 24)),
            "123 Street",
            null,
            UserStatus.INACTIVE,
            now,
            now,
            null,
            null
        );
    }
}
