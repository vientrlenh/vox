package com.sep.vox.application.port.input.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.command.LoginCommand;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.LoginResponseMapper;
import com.sep.vox.application.port.input.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.AuthenticationManagerPort;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.LoginResponse;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class LoginUseCase implements IUseCase<LoginCommand, LoginResponse> {

    private final AuthenticationManagerPort authenticationManagerPort;
    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AuthTokenPort authTokenPort;
    private final SessionManagerPort sessionManagerPort;

    public LoginUseCase(AuthenticationManagerPort authenticationManagerPort, 
                        UserRepository userRepository, 
                        UserRoleQueryRepository userRoleQueryRepository,
                        AuthTokenPort authTokenPort, 
                        SessionManagerPort sessionManagerPort) {
        this.authenticationManagerPort = authenticationManagerPort;
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.authTokenPort = authTokenPort;
        this.sessionManagerPort = sessionManagerPort;
    }

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
        var accessToken = authTokenPort.generateJwtToken(user.getId().toString(), userRoles);
        var refreshToken = sessionManagerPort.setSessionAndGetRefreshTokenWhenLogin(user.getId());
        return LoginResponseMapper.toResponse(accessToken, refreshToken);
    }
    
}
