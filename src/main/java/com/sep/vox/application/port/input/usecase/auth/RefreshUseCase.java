package com.sep.vox.application.port.input.usecase.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.auth.RefreshResponseMapper;
import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;

@Service
public class RefreshUseCase implements IUseCase<RefreshCommand, RefreshResponse> {

    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final SessionManagerPort sessionManagerPort;

    public RefreshUseCase(DeviceSessionRepository deviceSessionRepository, RefreshTokenRepository refreshTokenRepository, SessionTokenManagerPort sessionTokenManagerPort, SessionManagerPort sessionManagerPort) {
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.sessionManagerPort = sessionManagerPort;
    }

    @Override
    @Transactional
    public RefreshResponse execute(RefreshCommand input) {
        var command = normalize(input);
        
        var tokenHash = sessionTokenManagerPort.hash(command.token());
        var refreshToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Token yêu cầu không hợp lệ"));
        
        var now = OffsetDateTime.now();
        var deviceSession = deviceSessionRepository.findById(refreshToken.getSessionId())
            .orElseThrow(() -> new UnauthorizedException("Token yêu cầu không hợp lệ"));

        validateValidRequest(refreshToken, deviceSession, now, command);
        
        var newToken = sessionTokenManagerPort.generateToken();
        var newRefreshToken = createNewRefreshToken(newToken, deviceSession, refreshToken, now);
        markOldTokenAsUsed(refreshToken.getId(), newRefreshToken.getId(), deviceSession.getId(), now);
        return RefreshResponseMapper.toResponse(newToken.rawToken());
    }
    
    private RefreshCommand normalize(RefreshCommand input) {
        return new RefreshCommand(
            StringNormalization.trimAndCollapseSpaces(input.token()), 
            StringNormalization.trimAndCollapseSpaces(input.deviceId())
        );
    }



    private void validateValidRequest(RefreshToken refreshToken, DeviceSession deviceSession, OffsetDateTime now, RefreshCommand command) {
        if (refreshToken.isExpired(now)) {
            throw new UnauthorizedException("Token yêu cầu không hợp lệ");
        }
        
        if (refreshToken.isUsed()) {
            sessionManagerPort.revoke(deviceSession.getId(), now);
            throw new UnauthorizedException("Token yêu cầu không hợp lệ");
        }

        if (deviceSession.isDeviceIdMismatches(command.deviceId())) {
            sessionManagerPort.revoke(deviceSession.getId(), now);
            throw new UnauthorizedException("Token yêu cầu không hợp lệ");
        }

        if (deviceSession.isRevoked()) {
            throw new UnauthorizedException("Token yêu cầu không hợp lệ");
        }
    }

    private RefreshToken createNewRefreshToken(GeneratedSessionToken newToken, DeviceSession deviceSession, RefreshToken refreshToken, OffsetDateTime now) {
        var newRefreshToken = RefreshToken.createFresh(deviceSession.getId(), newToken.hashedToken(), now);
        return refreshTokenRepository.save(newRefreshToken);
    }

    private void markOldTokenAsUsed(UUID oldTokenId, UUID newTokenId, UUID sessionId, OffsetDateTime now) {
        var updatedRow = refreshTokenRepository.markUsedAndReplacedBy(oldTokenId, newTokenId, now);
        if (updatedRow == 0) {
            sessionManagerPort.revoke(sessionId, now);
            throw new UnauthorizedException("Token yêu cầu không hợp lệ");
        }
    }
}
