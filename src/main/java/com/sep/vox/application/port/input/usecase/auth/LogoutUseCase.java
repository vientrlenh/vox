package com.sep.vox.application.port.input.usecase.auth;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.domain.repository.RefreshTokenRepository;

@Service
public class LogoutUseCase implements IUseCase<LogoutCommand, Void> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final SessionManagerPort sessionManagerPort;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, SessionTokenManagerPort sessionTokenManagerPort, SessionManagerPort sessionManagerPort) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.sessionManagerPort = sessionManagerPort;
    }

    @Override
    @Transactional
    public Void execute(LogoutCommand input) {
        var tokenHash = sessionTokenManagerPort.hash(input.token());
        refreshTokenRepository.findByTokenHash(tokenHash)
            .ifPresent(refreshToken -> sessionManagerPort.revoke(refreshToken.getSessionId(), OffsetDateTime.now()));
        return null;
    }
}
