package com.sep.vox.application.port.input.usecase.auth;

import com.sep.vox.domain.model.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.auth.RefreshResponseMapper;
import com.sep.vox.application.port.input.command.auth.RefreshCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class RefreshUseCase implements IUseCase<RefreshCommand, RefreshResponse> {

    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final SessionManagerPort sessionManagerPort;
    private final AuthTokenPort authTokenPort;
    private final SchoolUserRepository schoolUserRepository;


    @Override
    @Transactional
    public RefreshResponse execute(RefreshCommand input) {
        RefreshCommand command = normalize(input);
        
        String tokenHash = sessionTokenManagerPort.hash(command.token());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("The requested token is invalid"));
        
        Instant now = Instant.now();
        DeviceSession deviceSession = deviceSessionRepository.findById(refreshToken.getSessionId())
            .orElseThrow(() -> new UnauthorizedException("The requested token is invalid"));

        validateValidRequest(refreshToken, deviceSession, now, command);
        
        GeneratedSessionToken newRefreshToken = sessionTokenManagerPort.generateToken();
        RefreshToken newGeneratedRefreshToken = createNewRefreshToken(newRefreshToken, deviceSession, refreshToken, now);
        markOldTokenAsUsed(refreshToken.getId(), newGeneratedRefreshToken.getId(), deviceSession.getId(), now);

        User user = userRepository.findById(deviceSession.getUserId())
            .orElseThrow(() -> new UnauthorizedException("The requested token is invalid"));
        UUID schoolId = schoolUserRepository.findByUserId(user.getId())
            .map(su -> su.getSchoolId())
            .orElse(null);
        String accessToken = authTokenPort.generateJwtToken(user.getId().toString(), schoolId, user.getEmail().value(), getUserRoles(user.getId()));
        return RefreshResponseMapper.toResponse(accessToken, newRefreshToken.rawToken());
    }
    
    private RefreshCommand normalize(RefreshCommand input) {
        return new RefreshCommand(
            StringNormalization.trimAndCollapseSpaces(input.token()), 
            StringNormalization.trimAndCollapseSpaces(input.deviceId())
        );
    }



    private void validateValidRequest(RefreshToken refreshToken, DeviceSession deviceSession, Instant now, RefreshCommand command) {
        if (refreshToken.isExpired(now)) {
            throw new UnauthorizedException("The requested token is invalid");
        }
        
        if (refreshToken.isUsed()) {
            sessionManagerPort.revoke(deviceSession.getId(), now);
            throw new UnauthorizedException("The requested token is invalid");
        }

        if (deviceSession.isDeviceIdMismatches(command.deviceId())) {
            sessionManagerPort.revoke(deviceSession.getId(), now);
            throw new UnauthorizedException("The requested token is invalid");
        }

        if (deviceSession.isRevoked()) {
            throw new UnauthorizedException("The requested token is invalid");
        }
    }

    private RefreshToken createNewRefreshToken(GeneratedSessionToken newToken, DeviceSession deviceSession, RefreshToken refreshToken, Instant now) {
        RefreshToken newRefreshToken = RefreshToken.createFresh(deviceSession.getId(), newToken.hashedToken(), now);
        return refreshTokenRepository.save(newRefreshToken);
    }

    private void markOldTokenAsUsed(UUID oldTokenId, UUID newTokenId, UUID sessionId, Instant now) {
        int updatedRow = refreshTokenRepository.markUsedAndReplacedBy(oldTokenId, newTokenId, now);
        if (updatedRow == 0) {
            sessionManagerPort.revoke(sessionId, now);
            throw new UnauthorizedException("The requested token is invalid");
        }
    }

    private List<String> getUserRoles(UUID userId) {
        return userRoleQueryRepository.findByUserIdWithRoleInfo(userId)
            .stream()
            .map(ur -> ur.roleCode())
            .toList();
    }
}
