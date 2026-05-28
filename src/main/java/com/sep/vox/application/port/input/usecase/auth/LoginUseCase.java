package com.sep.vox.application.port.input.usecase.auth;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.auth.LoginResponseMapper;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.domain.model.session.Session;
import com.sep.vox.domain.repository.SessionRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class LoginUseCase implements IUseCase<LoginCommand, LoginResponse> {

    private final AuthenticationManagerPort authenticationManagerPort;
    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AuthTokenPort authTokenPort;
    private final SessionManagerPort sessionManagerPort;
    private final SessionRepository sessionRepository;

    public LoginUseCase(AuthenticationManagerPort authenticationManagerPort, 
                        UserRepository userRepository, 
                        UserRoleQueryRepository userRoleQueryRepository,
                        AuthTokenPort authTokenPort, 
                        SessionManagerPort sessionManagerPort, 
                        SessionRepository sessionRepository) {
        this.authenticationManagerPort = authenticationManagerPort;
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.authTokenPort = authTokenPort;
        this.sessionManagerPort = sessionManagerPort;
        this.sessionRepository = sessionRepository;
    }

    private static final int REFRESH_TOKEN_DAY_TILL_EXPIRES = 3;

    @Override
    @Transactional
    public LoginResponse execute(LoginCommand input) {
        var email = authenticationManagerPort.setAuthenticationAndGetUserEmail(input.login(), input.password());
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        var userRoles = userRoleQueryRepository.findByUserIdWithRoleInfo(user.getId())
            .stream()
            .map(ur -> ur.roleCode())
            .toList();
        var accessToken = authTokenPort.generateJwtToken(user.getId().toString(), user.getEmail().value(), userRoles);
        var sessionToken = sessionManagerPort.generateToken();
        var now = OffsetDateTime.now();
        var session = new Session(
            user.getId(), 
            sessionToken.hashedToken(), 
            now, 
            now.plusDays(REFRESH_TOKEN_DAY_TILL_EXPIRES), 
            null, 
            null
        );
        sessionRepository.save(session);
        return LoginResponseMapper.toResponse(accessToken, sessionToken.rawToken(), userRoles);
    }
    
}
