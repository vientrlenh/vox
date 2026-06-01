package com.sep.vox.application.usecase.auth;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.LoginGoogleCommand;
import com.sep.vox.application.port.input.usecase.auth.LoginGoogleUseCase;
import com.sep.vox.application.port.output.AuthTokenPort;
import com.sep.vox.application.port.output.GoogleAuthPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.output.GeneratedSessionToken;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.role.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.valueobject.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginGoogleUseCaseTests {

    @Mock private GoogleAuthPort googleAuthPort;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleQueryRepository userRoleQueryRepository;
    @Mock private AuthTokenPort authTokenPort;
    @Mock private SessionTokenManagerPort sessionTokenManagerPort;
    @Mock private DeviceSessionRepository deviceSessionRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;

    @InjectMocks
    private LoginGoogleUseCase loginGoogleUseCase;

    private LoginGoogleCommand validCommand;
    private GoogleAuthPort.GoogleUserInfo validGoogleUser;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // Setup Dữ liệu mẫu
        ClientDeviceCommand deviceCommand = new ClientDeviceCommand("device-id", "test-device", "WEB");
        validCommand = new LoginGoogleCommand("valid-id-token", "192.168.1.1", "Chrome", deviceCommand);

        validGoogleUser = new GoogleAuthPort.GoogleUserInfo(
                "student@fpt.edu.vn", "Nguyen Van A", "avatar.png", true
        );

        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail(new Email("student@fpt.edu.vn"));
    }

    @Test
    @DisplayName("Ngoại lệ: Đăng nhập thất bại khi Email Google chưa được verify")
    void execute_EmailNotVerified_ThrowsUnauthorizedException() {
        GoogleAuthPort.GoogleUserInfo unverifiedUser = new GoogleAuthPort.GoogleUserInfo(
                "fake@fpt.edu.vn", "Hacker", "", false
        );
        when(googleAuthPort.verifyToken(validCommand.idToken())).thenReturn(unverifiedUser);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            loginGoogleUseCase.execute(validCommand);
        });

        assertEquals("Email Google này chưa được xác minh bởi Google.", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Thành công: Đăng nhập với User CŨ đã có trong Database")
    void execute_ExistingUser_Success() {
        when(googleAuthPort.verifyToken(validCommand.idToken())).thenReturn(validGoogleUser);
        when(userRepository.findByEmail(validGoogleUser.email())).thenReturn(Optional.of(mockUser));

        setupMockTokensAndSessions();

        LoginResponse response = loginGoogleUseCase.execute(validCommand);

        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        // Sửa thành refreshToken() cho khớp với LoginResponse record
        assertEquals("mock-session-token", response.refreshToken());

        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByCode(anyString());
    }



    private void setupMockTokensAndSessions() {
        UserRoleInfo roleInfo = new UserRoleInfo(
                1L,                               // id của record UserRole
                mockUser.getId(),                 // userId
                UUID.randomUUID(),                // roleId
                java.time.OffsetDateTime.now(),   // assignedAt (thời gian gán role)
                "STUDENT",                        // roleCode
                "Sinh viên"                       // roleName
        );
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(mockUser.getId()))
                .thenReturn(List.of(roleInfo));

        DeviceSession mockSession = mock(DeviceSession.class);
        when(mockSession.getId()).thenReturn(UUID.randomUUID());
        when(deviceSessionRepository.save(any())).thenReturn(mockSession);

        when(authTokenPort.generateJwtToken(anyString(), anyString(), anyList()))
                .thenReturn("mock-access-token");

        GeneratedSessionToken mockSessionToken = new GeneratedSessionToken("mock-session-token", "hashed-token");
        when(sessionTokenManagerPort.generateToken()).thenReturn(mockSessionToken);
    }
}