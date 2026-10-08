package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.SetUpPasswordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class SetUpPasswordUseCase implements IUseCase<SetUpPasswordCommand, Void>{

    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final UserRepository userRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public SetUpPasswordUseCase(
        PasswordSetUpTokenRepository passwordSetUpTokenRepository, 
        UserRepository userRepository,
        PasswordSetUpTokenPort passwordSetUpTokenPort,  
        PasswordEncoderPort passwordEncoderPort
    ) {
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.userRepository = userRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public Void execute(SetUpPasswordCommand input) {
        SetUpPasswordCommand normalized = normalize(input);

        Instant now = Instant.now();
        String hashedToken = passwordSetUpTokenPort.hash(normalized.token());
        
        int rowAffected = passwordSetUpTokenRepository.updateUsedToken(normalized.userId(), hashedToken, now);
        if (rowAffected == 0) {
            throw new UnauthorizedException("Invalid or expired password set up token");
        }

        String hashedPassword = passwordEncoderPort.hash(input.password());
        User user = userRepository.findByIdForUpdate(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.updatePasswordAndActivate(hashedPassword, now);
        userRepository.save(user);
        return null;
    }
    
    private SetUpPasswordCommand normalize(SetUpPasswordCommand input) {
        return new SetUpPasswordCommand(
            input.userId(), 
            StringNormalization.trimAndCollapseSpaces(input.token()), 
            input.password()
        );
    }
}
