package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.projection.repository.SchoolUserProjectionRepository;
import com.sep.vox.application.projection.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.AuthenticatedInfo;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;

@Service
public class LoginUseCase implements IUseCase<LoginCommand, LoginResponse> {


    private final UserRepository userRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SchoolUserProjectionRepository schoolUserProjectionRepository;
    private final AuthenticationManagerPort authenticationManagerPort;
    private final AuthTokenPort authTokenPort;
    private final SessionTokenManagerPort sessionTokenManagerPort;

    public LoginUseCase(UserRepository userRepository,  
                        DeviceSessionRepository deviceSessionRepository, 
                        RefreshTokenRepository refreshTokenRepository,
                        SchoolUserProjectionRepository schoolUserProjectionRepository,
                        AuthenticationManagerPort authenticationManagerPort, 
                        AuthTokenPort authTokenPort, 
                        SessionTokenManagerPort sessionTokenManagerPort) {
        this.userRepository = userRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.schoolUserProjectionRepository = schoolUserProjectionRepository;
        this.authenticationManagerPort = authenticationManagerPort;
        this.authTokenPort = authTokenPort;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
    }

    @Override
    @Transactional
    public LoginResponse execute(LoginCommand input) {
        LoginCommand command = normalize(input);
        Instant now = Instant.now();

        AuthenticatedInfo userInfo = authenticationManagerPort.setAuthenticationAndGetInfo(command.login(), command.password());
        User user = userRepository.findById(userInfo.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        DeviceSession deviceSession = createDeviceSession(user.getId(), command);
        UUID schoolId = null;
        if (user.isSystemAdmin()) {
            schoolId = schoolUserProjectionRepository.findSchoolIdByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User is not in any school"));
        }
        String accessToken = authTokenPort.generateToken(
            user.getId(), 
            schoolId, 
            Email.valueOf(user.getEmail()), 
            user.roleStrs()
        );
        SessionToken sessionToken = createSessionToken();
        createRefreshToken(deviceSession, sessionToken, now);

        return LoginResponse.toResponse(accessToken, sessionToken.rawToken(), Email.valueOf(user.getEmail()));
    }

    private LoginCommand normalize(LoginCommand input) {
        return new LoginCommand(
            StringNormalization.trimAndCollapseSpaces(input.login()), 
            input.password(), 
            StringNormalization.trimAndCollapseSpaces(input.ipAddress()),
            StringNormalization.trimAndCollapseSpaces(input.userAgent()),
            StringNormalization.trimAndCollapseSpaces(input.deviceId()),
            StringNormalization.trimAndCollapseSpaces(input.deviceName()),
            input.platform()
        );
    }

    private SessionToken createSessionToken() {
        SessionToken sessionToken = sessionTokenManagerPort.generateToken();
        while (refreshTokenRepository.existsByTokenHash(sessionToken.hashedToken())) {
            SessionToken newSessionToken = sessionTokenManagerPort.generateToken();
            sessionToken = newSessionToken;
        }
        return sessionToken;
    }

    private DeviceSession createDeviceSession(UUID userId, LoginCommand command) {
        DeviceSession deviceSession = DeviceSession.create(
            userId, 
            command.deviceId(), 
            command.deviceName(), 
            command.platform(),
            command.ipAddress(), 
            command.userAgent()
        );
        return deviceSessionRepository.save(deviceSession);
    }

    private void createRefreshToken(DeviceSession deviceSession, SessionToken sessionToken, Instant now) {
        RefreshToken refreshToken = RefreshToken.createFresh(deviceSession.getId(), sessionToken.hashedToken(), now);
        refreshTokenRepository.save(refreshToken);
    }
}
