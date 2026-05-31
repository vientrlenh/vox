package com.sep.vox.application.port.input.usecase.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.mapper.auth.LoginResponseMapper;
import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.LoginGoogleCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.GoogleAuthPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.Email;

@Service
public class LoginGoogleUseCase implements IUseCase<LoginGoogleCommand, LoginResponse> {

    private final GoogleAuthPort googleAuthPort;
    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final AuthTokenPort authTokenPort;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final DeviceSessionRepository deviceSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginGoogleUseCase(GoogleAuthPort googleAuthPort,
                              UserRepository userRepository,
                              UserRoleQueryRepository userRoleQueryRepository,
                              AuthTokenPort authTokenPort,
                              SessionTokenManagerPort sessionTokenManagerPort,
                              DeviceSessionRepository deviceSessionRepository,
                              RefreshTokenRepository refreshTokenRepository) {
        this.googleAuthPort = googleAuthPort;
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.authTokenPort = authTokenPort;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public LoginResponse execute(LoginGoogleCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();

        // 1. Xác minh Token với Google
        GoogleAuthPort.GoogleUserInfo googleUser = googleAuthPort.verifyToken(command.idToken());

        if (!googleUser.emailVerified()) {
            throw new UnauthorizedException("Email Google này chưa được xác minh bởi Google.");
        }

        // 2. Chuyển đổi sang Value Object
        Email email = new Email(googleUser.email());

        // 3. Tìm User trong DB
        var user = userRepository.findByEmail(email.value()).orElseThrow(() ->
                new UnauthorizedException("Tài khoản chưa được đăng ký trong hệ thống VOX.")
        );

        // 4. Lấy danh sách Role thông qua Query Repository
        var userRoles = getUserRoles(user.getId());

        // 5. Sinh JWT Token & Session (Giữ nguyên luồng chuẩn của VOX)
        var deviceSession = createDeviceSession(user.getId(), command);
        var accessToken = authTokenPort.generateJwtToken(user.getId().toString(), user.getEmail().value(), userRoles);
        var sessionToken = sessionTokenManagerPort.generateToken();
        createRefreshToken(deviceSession, sessionToken, now);

        return LoginResponseMapper.toResponse(accessToken, sessionToken.rawToken(), userRoles);
    }

    // ==========================================
    // CÁC HÀM HELPER TƯƠNG TỰ NHƯ LOGIN BÌNH THƯỜNG
    // ==========================================

    private LoginGoogleCommand normalize(LoginGoogleCommand input) {
        return new LoginGoogleCommand(
                input.idToken(),
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

    private DeviceSession createDeviceSession(UUID userId, LoginGoogleCommand command) {
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