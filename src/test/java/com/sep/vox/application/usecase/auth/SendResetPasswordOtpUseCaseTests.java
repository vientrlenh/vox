package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.event.ResetPasswordOtpRequestedPayloadV1;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.auth.SendResetPasswordOtpUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class SendResetPasswordOtpUseCaseTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private JsonSerializationPort jsonSerializationPort;
    @InjectMocks
    private SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;

    private final UUID userId = UUID.randomUUID();

    @Test
    void send_reset_password_otp_should_write_outbox_event_when_email_exists() {
        SendResetPasswordOtpCommand command = new SendResetPasswordOtpCommand(" Admin@Example.COM ");
        String normalizedEmail = "admin@example.com";

        when(userRepository.findByEmailAndStatus(normalizedEmail, UserStatus.ACTIVE))
            .thenReturn(Optional.of(user()));
        givenPayloadSerializesToJson();

        Void result = sendResetPasswordOtpUseCase.execute(command);

        assertThat(result).isNull();

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType())
            .isEqualTo(EventTypeConstant.RESET_PASSWORD_OTP_REQUESTED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(userId);
    }

    /**
     * This is the security property of the whole design: the OTP is generated in the consumer right
     * before the mail is sent, so the payload that goes into the outboxes table (which is backed up)
     * and Kafka (kept per retention) may carry only the recipient address. Adding the code to the
     * payload turns a credential into durable data.
     */
    @Test
    void send_reset_password_otp_payload_must_carry_only_the_recipient_address() {
        SendResetPasswordOtpCommand command = new SendResetPasswordOtpCommand("admin@example.com");

        when(userRepository.findByEmailAndStatus("admin@example.com", UserStatus.ACTIVE))
            .thenReturn(Optional.of(user()));
        givenPayloadSerializesToJson();

        sendResetPasswordOtpUseCase.execute(command);

        ArgumentCaptor<ResetPasswordOtpRequestedPayloadV1> captor = ArgumentCaptor.forClass(ResetPasswordOtpRequestedPayloadV1.class);
        verify(jsonSerializationPort).toJson(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
            new ResetPasswordOtpRequestedPayloadV1("admin@example.com"));
        assertThat(ResetPasswordOtpRequestedPayloadV1.class.getRecordComponents())
            .as("the payload must not have any field besides the recipient address")
            .hasSize(1);
    }

    @Test
    void send_reset_password_otp_should_do_nothing_when_email_does_not_exist() {
        SendResetPasswordOtpCommand command = new SendResetPasswordOtpCommand(" Missing@Example.COM ");

        when(userRepository.findByEmailAndStatus("missing@example.com", UserStatus.ACTIVE))
            .thenReturn(Optional.empty());

        Void result = sendResetPasswordOtpUseCase.execute(command);

        assertThat(result).isNull();
        verify(userRepository).findByEmailAndStatus("missing@example.com", UserStatus.ACTIVE);
        verify(outboxRepository, never()).save(any());
    }

    private void givenPayloadSerializesToJson() {
        when(jsonSerializationPort.toJson(any())).thenReturn("{}");
    }

    private User user() {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
