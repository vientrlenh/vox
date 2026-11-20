package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

public class SendResetPasswordOtpUseCaseTests {

    private UserRepository userRepository;
    private OutboxRepository outboxRepository;
    private JsonSerializationPort jsonSerializationPort;
    private SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        jsonSerializationPort = mock(JsonSerializationPort.class);
        sendResetPasswordOtpUseCase = new SendResetPasswordOtpUseCase(
            userRepository,
            outboxRepository,
            jsonSerializationPort
        );

        userId = UUID.randomUUID();
        when(jsonSerializationPort.toJson(any())).thenReturn("{}");
    }

    @Test
    void send_reset_password_otp_should_write_outbox_event_when_email_exists() {
        var command = new SendResetPasswordOtpCommand(" Admin@Example.COM ");
        var normalizedEmail = "admin@example.com";

        when(userRepository.findByEmailAndStatus(normalizedEmail, UserStatus.ACTIVE))
            .thenReturn(Optional.of(user()));

        var result = sendResetPasswordOtpUseCase.execute(command);

        assertThat(result).isNull();

        var captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType())
            .isEqualTo(EventTypeConstant.RESET_PASSWORD_OTP_REQUESTED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(userId);
    }

    /**
     * Đây là tính chất bảo mật của cả thiết kế: OTP được sinh ở consumer ngay trước lúc gửi
     * mail, nên payload đi vào outboxes (có backup) và Kafka (giữ theo retention) chỉ được
     * mang địa chỉ nhận. Thêm mã vào payload là biến credential thành dữ liệu bền vững.
     */
    @Test
    void send_reset_password_otp_payload_must_carry_only_the_recipient_address() {
        var command = new SendResetPasswordOtpCommand("admin@example.com");

        when(userRepository.findByEmailAndStatus("admin@example.com", UserStatus.ACTIVE))
            .thenReturn(Optional.of(user()));

        sendResetPasswordOtpUseCase.execute(command);

        var captor = ArgumentCaptor.forClass(ResetPasswordOtpRequestedPayloadV1.class);
        verify(jsonSerializationPort).toJson(captor.capture());
        assertThat(captor.getValue()).isEqualTo(
            new ResetPasswordOtpRequestedPayloadV1("admin@example.com"));
        assertThat(ResetPasswordOtpRequestedPayloadV1.class.getRecordComponents())
            .as("payload không được có thêm trường nào ngoài địa chỉ nhận")
            .hasSize(1);
    }

    @Test
    void send_reset_password_otp_should_do_nothing_when_email_does_not_exist() {
        var command = new SendResetPasswordOtpCommand(" Missing@Example.COM ");

        when(userRepository.findByEmailAndStatus("missing@example.com", UserStatus.ACTIVE))
            .thenReturn(Optional.empty());

        var result = sendResetPasswordOtpUseCase.execute(command);

        assertThat(result).isNull();
        verify(userRepository).findByEmailAndStatus("missing@example.com", UserStatus.ACTIVE);
        verify(outboxRepository, never()).save(any());
    }

    private User user() {
        var user = new User();
        user.setId(userId);
        return user;
    }
}
