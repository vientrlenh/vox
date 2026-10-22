package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ResetPasswordOtpRequestedPayload;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.shared.AggregateTypeConstant;
import com.sep.vox.domain.shared.EventTypeConstant;

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

    @Override
    @Transactional
    public Void execute(SendResetPasswordOtpCommand input) {
        SendResetPasswordOtpCommand command = normalize(input);

        User user = userRepository.findByEmailAndStatus(command.email(), UserStatus.ACTIVE).orElse(null);
        if (user == null) {
            return null;
        }

        String payload = jsonSerializationPort.toJson(
            new ResetPasswordOtpRequestedPayload(command.email()));

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
