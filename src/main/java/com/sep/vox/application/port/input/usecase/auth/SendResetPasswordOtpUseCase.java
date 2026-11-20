package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.ResetPasswordOtpRequestedPayloadV1;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class SendResetPasswordOtpUseCase implements IUseCase<SendResetPasswordOtpCommand, Void> {

    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public SendResetPasswordOtpUseCase(
            UserRepository userRepository,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.userRepository = userRepository;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    /**
     * Use case này KHÔNG còn sinh OTP. Mã được sinh ở
     * {@code ResetPasswordOtpEmailConsumer} ngay trước lúc gửi mail, để nó không bao giờ
     * nằm trong outboxes.payload hay trong topic Kafka.
     */
    @Override
    @Transactional
    public Void execute(SendResetPasswordOtpCommand input) {
        var command = normalize(input);

        // Email lạ vẫn trả về như bình thường: khác biệt phản hồi ở đây là một kênh dò xem
        // địa chỉ nào có tài khoản.
        var user = userRepository.findByEmailAndStatus(command.email(), UserStatus.ACTIVE).orElse(null);
        if (user == null) {
            return null;
        }

        var payload = jsonSerializationPort.toJson(
            new ResetPasswordOtpRequestedPayloadV1(command.email()));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.USER,
            user.getId(),
            EventTypeConstant.RESET_PASSWORD_OTP_REQUESTED,
            payload,
            Instant.now()
        ));
        return null;
    }

    private SendResetPasswordOtpCommand normalize(SendResetPasswordOtpCommand input) {
        return new SendResetPasswordOtpCommand(
            StringNormalization.normalizeEmail(input.email())
        );
    }
}
