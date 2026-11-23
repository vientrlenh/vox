package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.auth.SetUpPasswordCommand;
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

@ExtendWith(MockitoExtension.class)
public class SetUpPasswordUseCaseTests {

    @Mock
    private PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    @Mock
    private PasswordSetUpTokenPort passwordSetUpTokenPort;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @InjectMocks
    private SetUpPasswordUseCase setUpPasswordUseCase;

    @Test
    void set_up_password_should_consume_token_update_password_and_activate_user() {
        UUID userId = UUID.randomUUID();
        SetUpPasswordCommand command = new SetUpPasswordCommand(userId, "raw-token", "new-password");
        User user = inactiveUser(userId);

        when(passwordSetUpTokenPort.hash("raw-token")).thenReturn("hashed-token");
        when(passwordSetUpTokenRepository.updateUsedToken(eq(userId), eq("hashed-token"), any(Instant.class)))
            .thenReturn(1);
        when(passwordEncoderPort.hash("new-password")).thenReturn("hashed-password");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Void result = setUpPasswordUseCase.execute(command);

        assertThat(result).isNull();
        verify(passwordSetUpTokenPort).hash("raw-token");

        ArgumentCaptor<Instant> tokenUsedAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(passwordSetUpTokenRepository).updateUsedToken(
            eq(userId),
            eq("hashed-token"),
            tokenUsedAtCaptor.capture()
        );
        assertThat(tokenUsedAtCaptor.getValue()).isNotNull();

        verify(passwordEncoderPort).hash("new-password");
        verify(userRepository).findById(userId);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isEqualTo(userId);
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void set_up_password_should_reject_when_token_is_invalid_expired_or_used() {
        UUID userId = UUID.randomUUID();
        SetUpPasswordCommand command = new SetUpPasswordCommand(userId, "raw-token", "new-password");

        when(passwordSetUpTokenPort.hash("raw-token")).thenReturn("hashed-token");
        when(passwordSetUpTokenRepository.updateUsedToken(eq(userId), eq("hashed-token"), any(Instant.class)))
            .thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> setUpPasswordUseCase.execute(command));

        verify(passwordSetUpTokenPort).hash("raw-token");
        verify(passwordSetUpTokenRepository).updateUsedToken(
            eq(userId),
            eq("hashed-token"),
            any(Instant.class)
        );
        verifyNoInteractions(passwordEncoderPort, userRepository);
    }

    @Test
    void set_up_password_should_throw_when_user_is_not_found_after_token_is_consumed() {
        UUID userId = UUID.randomUUID();
        SetUpPasswordCommand command = new SetUpPasswordCommand(userId, "raw-token", "new-password");

        when(passwordSetUpTokenPort.hash("raw-token")).thenReturn("hashed-token");
        when(passwordSetUpTokenRepository.updateUsedToken(eq(userId), eq("hashed-token"), any(Instant.class)))
            .thenReturn(1);
        when(passwordEncoderPort.hash("new-password")).thenReturn("hashed-password");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> setUpPasswordUseCase.execute(command));

        verify(passwordSetUpTokenPort).hash("raw-token");
        verify(passwordSetUpTokenRepository).updateUsedToken(
            eq(userId),
            eq("hashed-token"),
            any(Instant.class)
        );
        verify(passwordEncoderPort).hash("new-password");
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    private User inactiveUser(UUID userId) {
        Instant now = Instant.now();
        return User.builder()
            .id(userId)
            .email(new Email("admin@example.com"))
            .passwordHash("__PASSWORD_NOT_SET__")
            .phone(new Phone("0987654321"))
            .fullName(new FullName("Nguyen Van A"))
            .dateOfBirth(new DateOfBirth(LocalDate.of(2000, 5, 24)))
            .address("123 Street")
            .status(UserStatus.INACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
