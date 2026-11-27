package com.sep.vox.application.port.input.usecase.auth;

import com.sep.vox.domain.model.user.User;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.ResetPasswordOtpRequestedPayloadV1;
import com.sep.vox.application.port.input.command.auth.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class SendResetPasswordOtpUseCase implements IUseCase<SendResetPasswordOtpCommand, Void> {

    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    /**
     * This use case NO LONGER generates the OTP. The code is generated in
     * {@code ResetPasswordOtpEmailConsumer} right before the mail is sent, so it never appears in
     * outboxes.payload or in the Kafka topic.
     */
    @Override
    @Transactional
    public Void execute(SendResetPasswordOtpCommand input) {
        SendResetPasswordOtpCommand command = normalize(input);

        // An unknown email is answered the same way as a known one: a difference in the response here would be a
        // side channel for finding out which addresses have an account.
        User user = userRepository.findByEmailAndStatus(command.email(), UserStatus.ACTIVE).orElse(null);
        if (user == null) {
            return null;
        }

        String payload = jsonSerializationPort.toJson(
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
