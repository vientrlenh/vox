package com.sep.vox.application.port.input.usecase.auth;

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
        var now = Instant.now();
        var hashedToken = passwordSetUpTokenPort.hash(input.token());
        
        var updatedTokenRows = passwordSetUpTokenRepository.updateUsedToken(input.userId(), hashedToken, now);
        if (updatedTokenRows == 0) {
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }

        var passwordHash = passwordEncoderPort.hash(input.password());
        var user = userRepository.findById(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tỉm thấy người dùng"));
        user.updatePasswordAndActivate(passwordHash, now);
        userRepository.save(user);

        return null;
    }
    

}
