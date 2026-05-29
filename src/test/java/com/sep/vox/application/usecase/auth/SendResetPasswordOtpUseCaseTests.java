package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.event.SendResetPasswordOtpEvent;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.auth.SendResetPasswordOtpUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;

public class SendResetPasswordOtpUseCaseTests {

    private UserRepository userRepository;
    private CacheManagerPort cacheManagerPort;
    private OneTimePasswordPort oneTimePasswordPort;
    private EventPublisherPort eventPublisherPort;
    private SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        cacheManagerPort = mock(CacheManagerPort.class);
        oneTimePasswordPort = mock(OneTimePasswordPort.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        sendResetPasswordOtpUseCase = new SendResetPasswordOtpUseCase(
            userRepository,
            cacheManagerPort,
            oneTimePasswordPort,
            eventPublisherPort
        );
    }

    @Test
    void send_reset_password_otp_should_store_hashed_otp_and_publish_email_event_when_email_exists() {
        var command = new SendResetPasswordOtpCommand(" Admin@Example.COM ");
        var normalizedEmail = "admin@example.com";
        var expectedKey = CacheKey.RESET_PASSWORD_PREFIX + CacheKey.OTP_PREFIX + normalizedEmail;

        when(userRepository.existsByEmailAndStatus(normalizedEmail, UserStatus.ACTIVE)).thenReturn(true);
        when(oneTimePasswordPort.generate(7)).thenReturn("1234567");
        when(oneTimePasswordPort.hash("1234567")).thenReturn("hashed-otp");

        var result = sendResetPasswordOtpUseCase.execute(command);

        assertThat(result).isNull();
        verify(userRepository).existsByEmailAndStatus(normalizedEmail, UserStatus.ACTIVE);
        verify(oneTimePasswordPort).generate(7);
        verify(oneTimePasswordPort).hash("1234567");
        verify(cacheManagerPort).save(expectedKey, "hashed-otp", Duration.ofMinutes(5));
        verify(eventPublisherPort).publish(new SendResetPasswordOtpEvent(normalizedEmail, "1234567"));
    }

    @Test
    void send_reset_password_otp_should_return_success_without_generating_otp_when_email_does_not_exist() {
        var command = new SendResetPasswordOtpCommand("missing@example.com");

        when(userRepository.existsByEmailAndStatus("missing@example.com", UserStatus.ACTIVE)).thenReturn(false);

        var result = sendResetPasswordOtpUseCase.execute(command);

        assertThat(result).isNull();
        verify(userRepository).existsByEmailAndStatus("missing@example.com", UserStatus.ACTIVE);
        verifyNoInteractions(oneTimePasswordPort, cacheManagerPort, eventPublisherPort);
    }

    @Test
    void send_reset_password_otp_should_not_store_or_publish_when_email_does_not_exist_after_normalization() {
        var command = new SendResetPasswordOtpCommand(" Missing@Example.COM ");

        when(userRepository.existsByEmailAndStatus("missing@example.com", UserStatus.ACTIVE)).thenReturn(false);

        sendResetPasswordOtpUseCase.execute(command);

        verify(userRepository).existsByEmailAndStatus("missing@example.com", UserStatus.ACTIVE);
        verify(oneTimePasswordPort, never()).generate(7);
        verifyNoInteractions(cacheManagerPort, eventPublisherPort);
    }
}
