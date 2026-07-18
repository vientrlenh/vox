package com.sep.vox.application.usecase.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.usecase.auth.LogoutUseCase;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.RefreshTokenRepository;

class LogoutUseCaseTests {

    private RefreshTokenRepository refreshTokenRepository;
    private SessionTokenManagerPort sessionTokenManagerPort;
    private SessionManagerPort sessionManagerPort;
    private LogoutUseCase logoutUseCase;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        sessionTokenManagerPort = mock(SessionTokenManagerPort.class);
        sessionManagerPort = mock(SessionManagerPort.class);
        logoutUseCase = new LogoutUseCase(refreshTokenRepository, sessionTokenManagerPort, sessionManagerPort);
    }

    @Test
    void logout_should_revoke_session_when_token_is_known() {
        var sessionId = UUID.randomUUID();
        var refreshToken = new RefreshToken(sessionId, "hashed-token", OffsetDateTime.now(), OffsetDateTime.now().plusDays(7), null, null);

        when(sessionTokenManagerPort.hash("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(refreshToken));

        logoutUseCase.execute(new LogoutCommand("raw-token"));

        verify(sessionManagerPort).revoke(eq(sessionId), any(OffsetDateTime.class));
    }

    @Test
    void logout_should_do_nothing_when_token_is_unknown() {
        when(sessionTokenManagerPort.hash("raw-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.empty());

        logoutUseCase.execute(new LogoutCommand("raw-token"));

        verify(sessionManagerPort, never()).revoke(any(), any());
    }
}
