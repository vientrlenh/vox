package com.sep.vox.application.port.input.usecase.auth;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.SendResetPasswordOtpEvent;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class SendResetPasswordOtpUseCase implements IUseCase<SendResetPasswordOtpCommand, Void> {

    private final UserRepository userRepository;
    private final CacheManagerPort cacheManagerPort;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final EventPublisherPort eventPublisherPort;

    public SendResetPasswordOtpUseCase(
        UserRepository userRepository,
        CacheManagerPort cacheManagerPort, OneTimePasswordPort oneTimePasswordPort, EventPublisherPort eventPublisherPort) {
        this.userRepository = userRepository;
        this.cacheManagerPort = cacheManagerPort;
        this.oneTimePasswordPort = oneTimePasswordPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    private static final int OTP_SIZE = 7;
    private static final Duration TTL = Duration.ofMinutes(5);


    @Override
    public Void execute(SendResetPasswordOtpCommand input) {
        var command = normalize(input);

        if (!userRepository.existsByEmail(command.email())) {
            return null;
        }

        var otp = oneTimePasswordPort.generate(OTP_SIZE);
        var otpHash = oneTimePasswordPort.hash(otp);

        var key = CacheKey.RESET_PASSWORD_PREFIX + CacheKey.OTP_PREFIX + command.email();
        cacheManagerPort.save(key, otpHash, TTL);
        eventPublisherPort.publish(new SendResetPasswordOtpEvent(command.email(), otp));
        return null;
    }
    
    private SendResetPasswordOtpCommand normalize(SendResetPasswordOtpCommand input) {
        return new SendResetPasswordOtpCommand(
            StringNormalization.normalizeEmail(input.email())
        );
    }
}
