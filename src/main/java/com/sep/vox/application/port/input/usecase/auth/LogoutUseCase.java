package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.service.AuthService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.RefreshTokenProjectionRepository;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.repository.DeviceSessionRepository;

@Service
public class LogoutUseCase implements IUseCase<LogoutCommand, Void> {

    private final RefreshTokenProjectionRepository refreshTokenProjectionRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final UserContextPort userContextPort;
    private final AuthService authService;

    public LogoutUseCase(
        RefreshTokenProjectionRepository refreshTokenProjectionRepository, 
        DeviceSessionRepository deviceSessionRepository, 
        SessionTokenManagerPort sessionTokenManagerPort, 
        UserContextPort userContextPort, 
        AuthService authService
    ) {
        this.refreshTokenProjectionRepository = refreshTokenProjectionRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.userContextPort = userContextPort;
        this.authService = authService;
    }

    @Override
    @Transactional
    public Void execute(LogoutCommand input) {
        LogoutCommand command = normalize(input);
        Instant now = Instant.now();

        Set<UUID> sessionIds = new LinkedHashSet<>();
        findSessionByRefreshToken(command.refreshToken()).ifPresent(sessionIds::add);
        sessionIds.addAll(findLiveSessionsOnDevice(command.deviceId()));
        
        for (UUID sessionId : sessionIds) {
            authService.revokeDeviceSession(sessionId, now);
        }
        return null;
    }

    private LogoutCommand normalize(LogoutCommand input) {
        return new LogoutCommand(
            StringNormalization.trimAndCollapseSpaces(input.refreshToken()),
            StringNormalization.trimAndCollapseSpaces(input.deviceId())
        );
    }

    private Optional<UUID> findSessionByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = sessionTokenManagerPort.hash(refreshToken);
        return refreshTokenProjectionRepository.findDeviceSessionIdByTokenHash(tokenHash);
    }


    private List<UUID> findLiveSessionsOnDevice(String deviceId) {
        return userContextPort.getCurrentAuthenticatedUserId()
            .map(userId -> deviceSessionRepository.findByUserId(userId).stream()
                .filter(session -> !session.isRevoked())
                .filter(session -> !session.isDeviceIdMismatches(deviceId))
                .map(session -> session.getId())
                .toList())
            .orElseGet(List::of);
    }
}
