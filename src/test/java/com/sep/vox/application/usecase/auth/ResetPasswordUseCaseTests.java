package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ResetPasswordCommand;
import com.sep.vox.application.port.input.usecase.auth.ResetPasswordUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordUseCaseTests {

    @Mock
    private CacheManagerPort cacheManagerPort;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OneTimePasswordPort oneTimePasswordPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @InjectMocks
    private ResetPasswordUseCase resetPasswordUseCase;

    @Test
    void reset_password_should_verify_otp_update_password_and_delete_otp() {
        ResetPasswordCommand command = new ResetPasswordCommand(" Admin@Example.COM ", "new-password", " 1234567 ");
        String email = "admin@example.com";
        String key = resetPasswordKey(email);

        when(userRepository.existsByEmailAndStatus(email, UserStatus.ACTIVE)).thenReturn(true);
        when(cacheManagerPort.get(key)).thenReturn("hashed-otp");
        when(oneTimePasswordPort.hash("1234567")).thenReturn("hashed-otp");
        when(passwordEncoderPort.hash("new-password")).thenReturn("hashed-password");
        when(userRepository.changeUserPassword(email, "hashed-password")).thenReturn(1);

        Void result = resetPasswordUseCase.execute(command);

        assertThat(result).isNull();
        verify(userRepository).existsByEmailAndStatus(email, UserStatus.ACTIVE);
        verify(cacheManagerPort).get(key);
        verify(oneTimePasswordPort).hash("1234567");
        verify(passwordEncoderPort).hash("new-password");
        verify(userRepository).changeUserPassword(email, "hashed-password");
        verify(cacheManagerPort).delete(key);
    }

    @Test
    void reset_password_should_reject_when_active_user_does_not_exist() {
        ResetPasswordCommand command = new ResetPasswordCommand("missing@example.com", "new-password", "1234567");

        when(userRepository.existsByEmailAndStatus("missing@example.com", UserStatus.ACTIVE)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> resetPasswordUseCase.execute(command));

        verify(userRepository).existsByEmailAndStatus("missing@example.com", UserStatus.ACTIVE);
        verifyNoInteractions(cacheManagerPort, oneTimePasswordPort, passwordEncoderPort);
        verify(userRepository, never()).changeUserPassword("missing@example.com", "hashed-password");
    }

    @Test
    void reset_password_should_reject_when_otp_is_missing_or_expired() {
        ResetPasswordCommand command = new ResetPasswordCommand("admin@example.com", "new-password", "1234567");
        String key = resetPasswordKey("admin@example.com");

        when(userRepository.existsByEmailAndStatus("admin@example.com", UserStatus.ACTIVE)).thenReturn(true);
        when(cacheManagerPort.get(key)).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> resetPasswordUseCase.execute(command));

        verify(userRepository).existsByEmailAndStatus("admin@example.com", UserStatus.ACTIVE);
        verify(cacheManagerPort).get(key);
        verifyNoInteractions(oneTimePasswordPort, passwordEncoderPort);
        verify(userRepository, never()).changeUserPassword("admin@example.com", "hashed-password");
        verify(cacheManagerPort, never()).delete(key);
    }

    @Test
    void reset_password_should_reject_when_otp_does_not_match() {
        ResetPasswordCommand command = new ResetPasswordCommand("admin@example.com", "new-password", "0000000");
        String key = resetPasswordKey("admin@example.com");

        when(userRepository.existsByEmailAndStatus("admin@example.com", UserStatus.ACTIVE)).thenReturn(true);
        when(cacheManagerPort.get(key)).thenReturn("hashed-otp");
        when(oneTimePasswordPort.hash("0000000")).thenReturn("different-hash");

        assertThrows(UnauthorizedException.class, () -> resetPasswordUseCase.execute(command));

        verify(userRepository).existsByEmailAndStatus("admin@example.com", UserStatus.ACTIVE);
        verify(cacheManagerPort).get(key);
        verify(oneTimePasswordPort).hash("0000000");
        verifyNoInteractions(passwordEncoderPort);
        verify(userRepository, never()).changeUserPassword("admin@example.com", "hashed-password");
        verify(cacheManagerPort, never()).delete(key);
    }

    @Test
    void reset_password_should_reject_and_keep_otp_when_password_update_affects_no_rows() {
        ResetPasswordCommand command = new ResetPasswordCommand("admin@example.com", "new-password", "1234567");
        String key = resetPasswordKey("admin@example.com");

        when(userRepository.existsByEmailAndStatus("admin@example.com", UserStatus.ACTIVE)).thenReturn(true);
        when(cacheManagerPort.get(key)).thenReturn("hashed-otp");
        when(oneTimePasswordPort.hash("1234567")).thenReturn("hashed-otp");
        when(passwordEncoderPort.hash("new-password")).thenReturn("hashed-password");
        when(userRepository.changeUserPassword("admin@example.com", "hashed-password")).thenReturn(0);

        assertThrows(UnauthorizedException.class, () -> resetPasswordUseCase.execute(command));

        verify(userRepository).existsByEmailAndStatus("admin@example.com", UserStatus.ACTIVE);
        verify(cacheManagerPort).get(key);
        verify(oneTimePasswordPort).hash("1234567");
        verify(passwordEncoderPort).hash("new-password");
        verify(userRepository).changeUserPassword("admin@example.com", "hashed-password");
        verify(cacheManagerPort, never()).delete(key);
    }

    private String resetPasswordKey(String email) {
        return CacheKey.RESET_PASSWORD_PREFIX + CacheKey.OTP_PREFIX + email;
    }
}
