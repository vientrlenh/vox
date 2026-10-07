package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.service.AuthService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.application.projection.repository.SchoolUserProjectionRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.AuthenticatedInfo;
import com.sep.vox.application.response.output.SessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;

@Service
public class LoginUseCase implements IUseCase<LoginCommand, LoginResponse> {


    private final UserRepository userRepository;
    private final SchoolUserProjectionRepository schoolUserProjectionRepository;
    private final AuthenticationManagerPort authenticationManagerPort;
    private final AuthTokenPort authTokenPort;
    private final AuthService authService;

    public LoginUseCase(UserRepository userRepository,  
                        SchoolUserProjectionRepository schoolUserProjectionRepository,
                        AuthenticationManagerPort authenticationManagerPort, 
                        AuthTokenPort authTokenPort, 
                        AuthService authService
                    ) {
        this.userRepository = userRepository;
        this.schoolUserProjectionRepository = schoolUserProjectionRepository;
        this.authenticationManagerPort = authenticationManagerPort;
        this.authTokenPort = authTokenPort;
        this.authService = authService;
    }

    @Override
    @Transactional
    public LoginResponse execute(LoginCommand input) {
        LoginCommand command = normalize(input);
        Instant now = Instant.now();

        AuthenticatedInfo userInfo = authenticationManagerPort.setAuthenticationAndGetInfo(command.login(), command.password());
        User user = userRepository.findById(userInfo.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        DeviceSession deviceSession = authService.createDeviceSession(
            user.getId(), 
            command.deviceId(), 
            command.deviceName(), 
            command.platform(), 
            command.ipAddress(), 
            command.userAgent()
        );
        UUID schoolId = null;
        if (!user.isSystemAdmin()) {
            schoolId = schoolUserProjectionRepository.findSchoolIdByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User is not in any school"));
        }
        String accessToken = authTokenPort.generateToken(
            user.getId(), 
            schoolId, 
            Email.valueOf(user.getEmail()), 
            user.roleStrs()
        );
        SessionToken sessionToken = authService.createSessionToken();
        authService.createRefreshToken(sessionToken, deviceSession, null, now);

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

}
