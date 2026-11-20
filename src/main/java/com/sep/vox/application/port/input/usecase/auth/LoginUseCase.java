package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.auth.LoginResponseMapper;
import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class LoginUseCase implements IUseCase<LoginCommand, LoginResponse> {

    private final AuthenticationManagerPort authenticationManagerPort;
    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AuthTokenPort authTokenPort;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SchoolUserRepository schoolUserRepository;

    public LoginUseCase(AuthenticationManagerPort authenticationManagerPort, 
                        UserRepository userRepository, 
                        UserRoleQueryRepository userRoleQueryRepository,
                        AuthTokenPort authTokenPort, 
                        SessionTokenManagerPort sessionTokenManagerPort, 
                        DeviceSessionRepository deviceSessionRepository, 
                        RefreshTokenRepository refreshTokenRepository,
                        SchoolUserRepository schoolUserRepository) {
        this.authenticationManagerPort = authenticationManagerPort;
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.authTokenPort = authTokenPort;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public LoginResponse execute(LoginCommand input) {
        var command = normalize(input);
        var now = Instant.now();

        var userId = authenticationManagerPort.setAuthenticationAndGetUserId(command.login(), command.password());
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        var userRoles = getUserRoles(user.getId());
        
        var deviceSession = createDeviceSession(userId, command);
        var schoolId = schoolUserRepository.findByUserId(user.getId())
            .map(su -> su.getSchoolId())
            .orElse(null);
        var accessToken = authTokenPort.generateJwtToken(user.getId().toString(), schoolId, user.getEmail().value(), userRoles);
        var sessionToken = sessionTokenManagerPort.generateToken();
        createRefreshToken(deviceSession, sessionToken, now);

        return LoginResponseMapper.toResponse(accessToken, sessionToken.rawToken(), userRoles);
    }

    private LoginCommand normalize(LoginCommand input) {
        return new LoginCommand(
            StringNormalization.trimAndCollapseSpaces(input.login()), 
            input.password(), 
            StringNormalization.trimAndCollapseSpaces(input.ipAddress()),
            StringNormalization.trimAndCollapseSpaces(input.userAgent()),
            new ClientDeviceCommand(
                StringNormalization.trimAndCollapseSpaces(input.device().deviceId()),
                StringNormalization.trimAndCollapseSpaces(input.device().deviceName()),
                StringNormalization.trimAndCollapseSpaces(input.device().platform())
            )
        );
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

    private DeviceSession createDeviceSession(UUID userId, LoginCommand command) {
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

    private void createRefreshToken(DeviceSession deviceSession, GeneratedSessionToken sessionToken, Instant now) {
        var refreshToken = RefreshToken.createFresh(deviceSession.getId(), sessionToken.hashedToken(), now);
        refreshTokenRepository.save(refreshToken);
    }
}
