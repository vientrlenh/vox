package com.sep.vox.application.port.input.usecase.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.CacheKey;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.auth.ResetPasswordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase implements IUseCase<ResetPasswordCommand, Void>{

    private final CacheManagerPort cacheManagerPort;
    private final UserRepository userRepository;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    @Transactional
    public Void execute(ResetPasswordCommand input) {
        ResetPasswordCommand command = normalize(input);

        if (!userRepository.existsByEmailAndStatus(command.email(), UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Password change request failed");
        }
        String key = resetPasswordKey(command); 
        String otpHash = cacheManagerPort.get(key);
        if (otpHash == null) {
            throw new UnauthorizedException("Password change request failed");
        }
        String hashedFromRequest = oneTimePasswordPort.hash(command.otp());
        if (!otpHash.equals(hashedFromRequest)) {
            throw new UnauthorizedException("Password change request failed");
        }
        
        String passwordHash = passwordEncoderPort.hash(command.password());
        int updatedRows = userRepository.changeUserPassword(command.email(), passwordHash);
        if (updatedRows == 0) {
            throw new UnauthorizedException("Password change request failed");
        }
        cacheManagerPort.delete(key);
        return null;
    }
    
    private ResetPasswordCommand normalize(ResetPasswordCommand input) {
        return new ResetPasswordCommand(
            StringNormalization.normalizeEmail(input.email()), 
            input.password(), 
            StringNormalization.trimAndCollapseSpaces(input.otp())
        );
    }

    private String resetPasswordKey(ResetPasswordCommand command) {
        return CacheKey.RESET_PASSWORD_PREFIX + CacheKey.OTP_PREFIX + command.email();
    }
}
