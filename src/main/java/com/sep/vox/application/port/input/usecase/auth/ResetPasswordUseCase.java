package com.sep.vox.application.port.input.usecase.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.shared.CacheKey;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ResetPasswordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.CacheManagerPort;
import com.sep.vox.application.port.output.OneTimePasswordPort;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ResetPasswordUseCase implements IUseCase<ResetPasswordCommand, Void>{

    private final CacheManagerPort cacheManagerPort;
    private final UserRepository userRepository;
    private final OneTimePasswordPort oneTimePasswordPort;
    private final PasswordEncoderPort passwordEncoderPort;
    
    public ResetPasswordUseCase(CacheManagerPort cacheManagerPort, UserRepository userRepository, OneTimePasswordPort oneTimePasswordPort, PasswordEncoderPort passwordEncoderPort) {
        this.cacheManagerPort = cacheManagerPort;
        this.userRepository = userRepository;
        this.oneTimePasswordPort = oneTimePasswordPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    private static final String PASSWORD_CHANGE_FAIL_MSG = "Password request change failed";

    @Override
    @Transactional
    public Void execute(ResetPasswordCommand input) {
        ResetPasswordCommand command = normalize(input);

        if (!userRepository.existsByEmailAndStatusActive(command.email())) {
            throw new UnauthorizedException(PASSWORD_CHANGE_FAIL_MSG);
        }

        String key = CacheKey.resetPasswordKey(command.email()); 
        String otpHash = cacheManagerPort.get(key);
        if (otpHash == null || otpHash.isBlank()) {
            throw new UnauthorizedException(PASSWORD_CHANGE_FAIL_MSG);
        }
        String hashedOtpFromReq = oneTimePasswordPort.hash(command.otp());
        if (!otpHash.equals(hashedOtpFromReq)) {
            throw new UnauthorizedException(PASSWORD_CHANGE_FAIL_MSG);
        }
        
        String passwordHash = passwordEncoderPort.hash(command.password());
        int affectedRows = userRepository.changeUserPassword(command.email(), passwordHash);
        if (affectedRows == 0) {
            throw new UnauthorizedException(PASSWORD_CHANGE_FAIL_MSG);
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
}
