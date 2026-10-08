package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.application.port.input.service.AuthService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.projection.repository.SchoolUserProjectionRepository;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;

@Service
public class RefreshUseCase implements IUseCase<RefreshCommand, RefreshResponse> {

    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SchoolUserProjectionRepository schoolUserProjectionRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final AuthTokenPort authTokenPort;
    private final AuthService authService;


    public RefreshUseCase(
        DeviceSessionRepository deviceSessionRepository, 
        RefreshTokenRepository refreshTokenRepository, 
        UserRepository userRepository, 
        SchoolUserProjectionRepository schoolUserProjectionRepository,
        SessionTokenManagerPort sessionTokenManagerPort,  
        AuthTokenPort authTokenPort, 
        AuthService authService
    ) {
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.schoolUserProjectionRepository = schoolUserProjectionRepository;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.authTokenPort = authTokenPort; 
        this.authService = authService;
    }

    private static final String INVALID_REFRESH_TOKEN_MSG = "Invalid requested refresh token";

    @Override
    @Transactional
    public RefreshResponse execute(RefreshCommand input) {
        RefreshCommand command = normalize(input);

        String rawToken = sessionTokenManagerPort.getRawFromCookie(command.req());
        String hashedToken = sessionTokenManagerPort.hash(rawToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(hashedToken)
            .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN_MSG));
        
        Instant now = Instant.now();
        DeviceSession deviceSession = deviceSessionRepository.findById(refreshToken.getDeviceSessionId())
            .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN_MSG));

        validateValidRequest(refreshToken, deviceSession, now, command);
        
        SessionToken newSessionToken = sessionTokenManagerPort.generateToken(command.res());
        RefreshToken newRefreshToken = authService.createRefreshToken(newSessionToken, deviceSession, refreshToken, now);
        markOldTokenAsUsed(refreshToken.getId(), newRefreshToken.getId(), deviceSession.getId(), now);

        User user = userRepository.findById(deviceSession.getUserId())
            .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN_MSG));
        UUID schoolId = null;
        if (user.isSystemAdmin()) {
            schoolId = schoolUserProjectionRepository.findSchoolIdByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User is not found in any school"));
        }
        String accessToken = authTokenPort.generateToken(
            user.getId(), 
            schoolId, 
            Email.valueOf(user.getEmail()), 
            user.roleStrs()
        );
        return RefreshResponse.toResponse(accessToken, newSessionToken.rawToken());
    }
    
    private RefreshCommand normalize(RefreshCommand input) {
        return new RefreshCommand(
            StringNormalization.trimAndCollapseSpaces(input.deviceId()), 
            input.req(), 
            input.res()
        );
    }

    private void validateValidRequest(RefreshToken refreshToken, DeviceSession deviceSession, Instant now, RefreshCommand command) {
        if (refreshToken.isExpired(now)) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN_MSG);
        }
        
        if (refreshToken.isUsed() || deviceSession.isDeviceIdMismatches(command.deviceId())) {
            authService.revokeDeviceSession(deviceSession.getId(), now);
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN_MSG);
        }
        if (deviceSession.isRevoked()) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN_MSG);
        }
    }

    private void markOldTokenAsUsed(UUID oldTokenId, UUID newTokenId, UUID sessionId, Instant now) {
        int rowAffected = refreshTokenRepository.markUsedAndReplacedBy(oldTokenId, newTokenId, now);
        if (rowAffected == 0) {
            authService.revokeDeviceSession(sessionId, now);
            throw new UnauthorizedException("Invalid requested refresh token");
        }
    }
}
