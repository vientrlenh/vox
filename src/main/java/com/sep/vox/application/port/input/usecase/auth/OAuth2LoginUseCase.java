package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.OAuth2LoginCommand;
import com.sep.vox.application.port.input.service.AuthService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.ServletRequestManagerPort;
import com.sep.vox.application.projection.repository.SchoolUserProjectionRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;

@Service
public class OAuth2LoginUseCase implements IUseCase<OAuth2LoginCommand, LoginResponse>{

    private final UserRepository userRepository;
    private final SchoolUserProjectionRepository schoolUserProjectionRepository;
    private final AuthTokenPort authTokenPort;
    private final ServletRequestManagerPort servletRequestManagerPort;
    private final AuthService authService;

    public OAuth2LoginUseCase(
        UserRepository userRepository, 
        SchoolUserProjectionRepository schoolUserProjectionRepository, 
        AuthTokenPort authTokenPort, 
        ServletRequestManagerPort servletRequestManagerPort, 
        AuthService authService
    ) {
        this.userRepository = userRepository; 
        this.schoolUserProjectionRepository = schoolUserProjectionRepository;
        this.authTokenPort = authTokenPort; 
        this.servletRequestManagerPort = servletRequestManagerPort;
        this.authService = authService;
    }

    @Override
    @Transactional
    public LoginResponse execute(OAuth2LoginCommand input) {
        OAuth2LoginCommand command = normalize(input);

        String ipAddress = servletRequestManagerPort.getContextIpAddress(command.req());
        String userAgent = servletRequestManagerPort.getUserAgent(command.req());
        User user = userRepository.findByEmailAndStatus(command.email(), UserStatus.ACTIVE)
            .orElseThrow(() -> new UnauthorizedException("User is unavailable. Contact school for support"));

        Instant now = Instant.now();
        DeviceSession deviceSession = authService.createDeviceSession(
            user.getId(), 
            command.deviceId(), 
            command.deviceName(), 
            command.platform(), 
            ipAddress, 
            userAgent
        );
        UUID schoolId = null;
        if (!user.isSystemAdmin()) {
            schoolId = schoolUserProjectionRepository.findSchoolIdByUserId(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not in any school"));
        }
        String email = Email.valueOf(user.getEmail());
        String accessToken = authTokenPort.generateToken(user.getId(), schoolId, email, user.roleStrs());
        SessionToken sessionToken = authService.createSessionToken(command.res());
        authService.createRefreshToken(sessionToken, deviceSession, null, now);

        return LoginResponse.toResponse(email, accessToken);

    }
    
    private OAuth2LoginCommand normalize(OAuth2LoginCommand input) {
        return new OAuth2LoginCommand(
            input.provider(), 
            input.providerUserId(), 
            StringNormalization.normalizeEmail(input.email()), 
            input.emailVerified(), 
            input.fullName(), 
            input.avatarUrl(), 
            StringNormalization.trimAndCollapseSpaces(input.deviceId()), 
            StringNormalization.trimAndCollapseSpaces(input.deviceName()), 
            input.platform(), 
            input.req(), 
            input.res()
        );
    }
}
