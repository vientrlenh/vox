package com.sep.vox.application.port.input.usecase.auth;

import com.sep.vox.domain.model.user.User;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.auth.SetUpPasswordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class SetUpPasswordUseCase implements IUseCase<SetUpPasswordCommand, Void>{

    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    @Override
    @Transactional
    public Void execute(SetUpPasswordCommand input) {
        Instant now = Instant.now();
        String hashedToken = passwordSetUpTokenPort.hash(input.token());
        
        int updatedTokenRows = passwordSetUpTokenRepository.updateUsedToken(input.userId(), hashedToken, now);
        if (updatedTokenRows == 0) {
            throw new IllegalArgumentException("The token is invalid or has expired");
        }

        String passwordHash = passwordEncoderPort.hash(input.password());
        User user = userRepository.findById(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.updatePasswordAndActivate(passwordHash);
        userRepository.save(user);

        return null;
    }
    

}
