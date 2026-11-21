package com.sep.vox.application.port.input.usecase.auth;

import com.sep.vox.domain.model.user.User;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SetUpPasswordCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PasswordEncoderPort;
import com.sep.vox.application.port.output.PasswordSetUpTokenPort;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class SetUpPasswordUseCase implements IUseCase<SetUpPasswordCommand, Void>{

    private final PasswordSetUpTokenRepository passwordSetUpTokenRepository;
    private final PasswordSetUpTokenPort passwordSetUpTokenPort;
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoderPort;

    public SetUpPasswordUseCase(PasswordSetUpTokenRepository passwordSetUpTokenRepository, PasswordSetUpTokenPort passwordSetUpTokenPort, UserRepository userRepository, PasswordEncoderPort passwordEncoderPort) {
        this.passwordSetUpTokenRepository = passwordSetUpTokenRepository;
        this.passwordSetUpTokenPort = passwordSetUpTokenPort;
        this.userRepository = userRepository;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    @Transactional
    public Void execute(SetUpPasswordCommand input) {
        Instant now = Instant.now();
        String hashedToken = passwordSetUpTokenPort.hash(input.token());
        
        int updatedTokenRows = passwordSetUpTokenRepository.updateUsedToken(input.userId(), hashedToken, now);
        if (updatedTokenRows == 0) {
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }

        String passwordHash = passwordEncoderPort.hash(input.password());
        User user = userRepository.findById(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tỉm thấy người dùng"));
        user.updatePasswordAndActivate(passwordHash);
        userRepository.save(user);

        return null;
    }
    

}
