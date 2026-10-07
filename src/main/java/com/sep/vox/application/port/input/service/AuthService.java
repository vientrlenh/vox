package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.platform.Platform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;

@Service 
public class AuthService {
    
    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final PlatformTransactionManager platformTransactionManager;

    public AuthService(
        DeviceSessionRepository deviceSessionRepository, 
        RefreshTokenRepository refreshTokenRepository, 
        SessionTokenManagerPort sessionTokenManagerPort, 
        PlatformTransactionManager platformTransactionManager
    ) {
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository; 
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.platformTransactionManager = platformTransactionManager;
    }

    public RefreshToken createRefreshToken(SessionToken sessionToken, DeviceSession deviceSession, RefreshToken oldRefreshToken, Instant now) {
        Instant expiresAt = now.plus(sessionToken.expirationMs(), ChronoUnit.MILLIS);
        RefreshToken refreshToken;
        if (oldRefreshToken == null) {
            refreshToken = RefreshToken.createFresh(deviceSession.getId(), sessionToken.hashedToken(), expiresAt);
        } else {
            refreshToken = RefreshToken.replace(deviceSession.getId(), sessionToken.hashedToken(), expiresAt, now, oldRefreshToken.getId());
        }
        return refreshTokenRepository.save(refreshToken);
    }


    public void revokeDeviceSession(UUID sessionId, Instant now) {
        TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> {
            DeviceSession sessionToBeRevoked = deviceSessionRepository.findById(sessionId)
                .orElse(null);
            int revokedRows = deviceSessionRepository.revokeDeviceSession(sessionId, now);
            if (sessionToBeRevoked == null || revokedRows == 0) {
                return;
            }
        });
    }


    public SessionToken createSessionToken() {
        SessionToken token = sessionTokenManagerPort.generateToken();
        while (refreshTokenRepository.existsByTokenHash(token.hashedToken())) {
            token = sessionTokenManagerPort.generateToken();
        }
        return token;
    }


    public DeviceSession createDeviceSession(UUID userId, String deviceId, String deviceName, Platform platform, String ipAddress, String userAgent) {
        DeviceSession deviceSession = DeviceSession.create(userId, deviceId, deviceName, platform, ipAddress, userAgent);
        return deviceSessionRepository.save(deviceSession);
    }
}
