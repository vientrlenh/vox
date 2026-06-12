package com.sep.vox.application.port.input.usecase.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.auth.LoginResponseMapper;
import com.sep.vox.application.port.input.command.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.application.service.UserSchoolResolver;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class OAuth2LoginUseCase implements IUseCase<OAuth2LoginCommand, LoginResponse>{

    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenPort authTokenPort;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final UserSchoolResolver userSchoolResolver;

    
    public OAuth2LoginUseCase(UserRepository userRepository, UserRoleQueryRepository userRoleQueryRepository, DeviceSessionRepository deviceSessionRepository, RefreshTokenRepository refreshTokenRepository, AuthTokenPort authTokenPort, SessionTokenManagerPort sessionTokenManagerPort, UserSchoolResolver userSchoolResolver) {
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authTokenPort = authTokenPort;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.userSchoolResolver = userSchoolResolver;
    }

    @Override
    @Transactional
    public LoginResponse execute(OAuth2LoginCommand input) {
        if (input.emailVerified() == null || !input.emailVerified().booleanValue()) {
            throw new UnauthorizedException("Người dùng chưa được xác thực để đăng nhập");
        }

        var user = userRepository.findByEmailAndStatus(input.email(), UserStatus.ACTIVE)
            .orElseThrow(() -> new UnauthorizedException("Người dùng hiện chưa tồn tại. Vui lòng gửi đơn đăng ký hoặc liên hệ bên nhà trường để được hỗ trợ"));

        var now = OffsetDateTime.now();
        var userRoles = getUserRoles(user.getId());
        var deviceSession = createDeviceSession(user.getId(), input);
        var schoolId = userSchoolResolver.findSchoolId(user.getId()).orElse(null);
        var accessToken = authTokenPort.generateJwtToken(user.getId().toString(), schoolId, user.getEmail().value(), userRoles);
        var sessionToken = sessionTokenManagerPort.generateToken();
        createRefreshToken(deviceSession, sessionToken, now);

        return LoginResponseMapper.toResponse(accessToken, sessionToken.rawToken(), userRoles);

    }
    
        private List<String> getUserRoles(UUID userId) {
        return userRoleQueryRepository.findByUserIdWithRoleInfo(userId)
            .stream()
            .map(ur -> ur.roleCode())
            .toList();
    }
    
    private SessionPlatform sessionPlatformFromRequest(String platform) {
        try {
            platform = platform.toUpperCase(Locale.ROOT);
            return SessionPlatform.valueOf(platform);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nền tảng " + platform + " hiện không được hỗ trợ");
        }
    }

    private DeviceSession createDeviceSession(UUID userId, OAuth2LoginCommand command) {
        var deviceSession = DeviceSession.create(
            userId, 
            command.device().deviceId(), 
            command.device().deviceName(), 
            sessionPlatformFromRequest(command.device().platform()), 
            command.ipAddress(), 
            command.userAgent()
        );
        return deviceSessionRepository.save(deviceSession);
    }

    private void createRefreshToken(DeviceSession deviceSession, GeneratedSessionToken sessionToken, OffsetDateTime now) {
        var refreshToken = RefreshToken.createFresh(deviceSession.getId(), sessionToken.hashedToken(), now);
        refreshTokenRepository.save(refreshToken);
    }
    
}
