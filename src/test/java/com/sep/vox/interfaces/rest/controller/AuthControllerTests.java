package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.application.port.input.command.ResetPasswordCommand;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.auth.GoogleTokenLoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.LogoutUseCase;
import com.sep.vox.application.port.input.usecase.auth.RefreshUseCase;
import com.sep.vox.application.port.input.usecase.auth.ResetPasswordUseCase;
import com.sep.vox.application.port.input.usecase.auth.SendResetPasswordOtpUseCase;
import com.sep.vox.application.port.input.usecase.auth.SetUpPasswordUseCase;
import com.sep.vox.application.port.input.usecase.registration.RegisterBySelfDeclaredUseCase;
import com.sep.vox.application.port.input.usecase.registration.RegisterFromSchoolDirectoryUseCase;
import com.sep.vox.application.port.input.usecase.registration.VerifyRegisterFormOtpUseCase;
import com.sep.vox.application.port.output.CookieManagerPort;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.interfaces.rest.dto.request.ClientDeviceRequest;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.DeviceIdRequest;
import com.sep.vox.interfaces.rest.dto.request.ResetPasswordRequest;
import com.sep.vox.interfaces.rest.dto.request.SendResetPasswordOtpRequest;

import jakarta.servlet.http.HttpServletRequest;

public class AuthControllerTests {

    private LoginUseCase loginUseCase;
    private RegisterFromSchoolDirectoryUseCase registerFromSchoolDirectoryUseCase;
    private SetUpPasswordUseCase setUpPasswordUseCase;
    private HttpServletRequest servletRequest;
    private RefreshUseCase refreshUseCase;
    private LogoutUseCase logoutUseCase;
    private SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;
    private ResetPasswordUseCase resetPasswordUseCase;
    private RegisterBySelfDeclaredUseCase registerBySelfDeclaredUseCase;
    private VerifyRegisterFormOtpUseCase verifyRegisterFormOtpUseCase;
    private GoogleTokenLoginUseCase googleTokenLoginUseCase;
    private CookieManagerPort cookieManagerPort;
    private AuthController authController;

    @BeforeEach
    void setup() {
        loginUseCase = mock(LoginUseCase.class);
        registerFromSchoolDirectoryUseCase = mock(RegisterFromSchoolDirectoryUseCase.class);
        setUpPasswordUseCase = mock(SetUpPasswordUseCase.class);
        servletRequest = mock(MockHttpServletRequest.class);
        refreshUseCase = mock(RefreshUseCase.class);
        logoutUseCase = mock(LogoutUseCase.class);
        sendResetPasswordOtpUseCase = mock(SendResetPasswordOtpUseCase.class);
        resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        registerBySelfDeclaredUseCase = mock(RegisterBySelfDeclaredUseCase.class);
        verifyRegisterFormOtpUseCase = mock(VerifyRegisterFormOtpUseCase.class);
        googleTokenLoginUseCase = mock(GoogleTokenLoginUseCase.class);
        cookieManagerPort = mock(CookieManagerPort.class);
        authController = new AuthController(loginUseCase, registerFromSchoolDirectoryUseCase, setUpPasswordUseCase, refreshUseCase, logoutUseCase, sendResetPasswordOtpUseCase, resetPasswordUseCase, registerBySelfDeclaredUseCase, verifyRegisterFormOtpUseCase, googleTokenLoginUseCase, cookieManagerPort);
    }


    @Test
    void login_should_return_ok_response() {
        var request = new LoginRequest(
            "admin@example.com",
            "password",
            new ClientDeviceRequest("device-1", "Chrome on Windows", "WEB")
        );
        var expectedCommand = new LoginCommand(
            request.login(),
            request.password(),
            "203.0.113.10",
            "JUnit User Agent",
            new ClientDeviceCommand(
                request.device().deviceId(),
                request.device().deviceName(),
                request.device().platform()
            )
        );
        var roles = List.of("STUDENT");
        var loginResponse = new LoginResponse("access-token", "refresh-token", roles);

        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(servletRequest.getHeader("User-Agent")).thenReturn("JUnit User Agent");
        when(servletRequest.getRemoteAddr()).thenReturn("203.0.113.10");
        when(loginUseCase.execute(expectedCommand))
            .thenReturn(loginResponse);

        var servletResponse = new MockHttpServletResponse();

        var response = authController.login(request, servletRequest, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new LoginResponse("access-token", null, roles));
        verify(cookieManagerPort).setCookie(servletResponse, "refresh_token", "refresh-token", 259200L);
        verify(loginUseCase).execute(expectedCommand);
    }

    @Test
    void refresh_should_return_ok_response() {
        var request = new DeviceIdRequest("device-1");
        var expectedCommand = new RefreshCommand("old-refresh-token", request.deviceId());
        var refreshResponse = new RefreshResponse("access-token", "new-refresh-token");

        when(refreshUseCase.execute(expectedCommand))
            .thenReturn(refreshResponse);

        var servletResponse = new MockHttpServletResponse();

        var response = authController.refresh(request, "old-refresh-token", servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new RefreshResponse("access-token", null));
        verify(cookieManagerPort).setCookie(servletResponse, "refresh_token", "new-refresh-token", 259200L);
        verify(refreshUseCase).execute(expectedCommand);
    }

    @Test
    void logout_should_revoke_session_and_clear_cookie() {
        var request = new DeviceIdRequest("device-1");
        var expectedCommand = new LogoutCommand("old-refresh-token", request.deviceId());

        var servletResponse = new MockHttpServletResponse();

        var response = authController.logout(request, "old-refresh-token", servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Đăng xuất thành công");
        verify(logoutUseCase).execute(expectedCommand);
        verify(cookieManagerPort).clearCookie(servletResponse, "refresh_token");
    }

    /**
     * Không có cookie vẫn phải là 200 kèm lệnh xoá cookie: client xoá token trong máy ngay sau
     * lời gọi này, nên một lỗi ở đây để lại client không còn token mà phiên thì vẫn sống.
     */
    @Test
    void logout_should_return_ok_response_when_refresh_cookie_is_missing() {
        var request = new DeviceIdRequest("device-1");
        var expectedCommand = new LogoutCommand(null, request.deviceId());

        var servletResponse = new MockHttpServletResponse();

        var response = authController.logout(request, null, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(logoutUseCase).execute(expectedCommand);
        verify(cookieManagerPort).clearCookie(servletResponse, "refresh_token");
    }

    @Test
    void send_reset_password_otp_should_return_ok_response() {
        var request = new SendResetPasswordOtpRequest("admin@example.com");
        var expectedCommand = new SendResetPasswordOtpCommand(request.email());

        var response = authController.sendResetPasswordOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Mã OTP đặt lại mật khẩu đã được gửi");
        assertThat(response.getBody().data()).isNull();
        verify(sendResetPasswordOtpUseCase).execute(expectedCommand);
    }

    @Test
    void reset_password_should_return_ok_response() {  
        var request = new ResetPasswordRequest("admin@example.com", "new-password", "1234567");
        var expectedCommand = new ResetPasswordCommand(request.email(), request.password(), request.otp());

        var response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Mật khẩu đã thay đổi thành công");
        assertThat(response.getBody().data()).isNull();
        verify(resetPasswordUseCase).execute(expectedCommand);
    }
}
