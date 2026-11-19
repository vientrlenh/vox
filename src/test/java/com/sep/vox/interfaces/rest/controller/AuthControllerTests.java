package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
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
import com.sep.vox.support.RestControllerTest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestControllerTest(AuthController.class)
class AuthControllerTests {

    private static final String LOGIN_JSON = """
        {"login": "admin@example.com", "password": "password",
         "device": {"deviceId": "device-1", "deviceName": "Chrome on Windows", "platform": "WEB"}}""";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LoginUseCase loginUseCase;
    @MockitoBean
    private RefreshUseCase refreshUseCase;
    @MockitoBean
    private LogoutUseCase logoutUseCase;
    @MockitoBean
    private SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;
    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;
    @MockitoBean
    private CookieManagerPort cookieManagerPort;

    // Required by the AuthController constructor; no test in this class calls them.
    @MockitoBean
    private RegisterFromSchoolDirectoryUseCase registerFromSchoolDirectoryUseCase;
    @MockitoBean
    private SetUpPasswordUseCase setUpPasswordUseCase;
    @MockitoBean
    private RegisterBySelfDeclaredUseCase registerBySelfDeclaredUseCase;
    @MockitoBean
    private VerifyRegisterFormOtpUseCase verifyRegisterFormOtpUseCase;
    @MockitoBean
    private GoogleTokenLoginUseCase googleTokenLoginUseCase;

    @Test
    void login_should_return_tokens_without_refresh_token_and_set_cookie() {
        var device = new ClientDeviceCommand("device-1", "Chrome on Windows", "WEB");
        var expectedCommand = new LoginCommand(
            "admin@example.com", "password", "203.0.113.10", "JUnit User Agent", device);
        when(loginUseCase.execute(expectedCommand))
            .thenReturn(new LoginResponse("access-token", "refresh-token", List.of("STUDENT")));

        var result = mvc.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "203.0.113.10")
            .header("User-Agent", "JUnit User Agent")
            .content(LOGIN_JSON)
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.data.accessToken").isEqualTo("access-token");
        assertThat(result).bodyJson().extractingPath("$.data.refreshToken").isNull();
        verify(cookieManagerPort).setCookie(
            any(HttpServletResponse.class), eq("refresh_token"), eq("refresh-token"), eq(259200L));
    }

    @Test
    void login_should_return_bad_request_and_skip_use_case_when_body_is_invalid() {
        var result = mvc.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\": \"\", \"password\": \"password\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.message").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(loginUseCase, cookieManagerPort);
    }

    @Test
    void login_should_map_unauthorized_exception_to_401_error_body() {
        when(loginUseCase.execute(any(LoginCommand.class)))
            .thenThrow(new UnauthorizedException("Invalid credentials"));

        var result = mvc.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(LOGIN_JSON)
            .exchange();

        assertThat(result).hasStatus(401).bodyJson().extractingPath("$.error").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(cookieManagerPort);
    }

    @Test
    void refresh_should_rotate_refresh_cookie_and_hide_new_token_from_body() {
        when(refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1")))
            .thenReturn(new RefreshResponse("access-token", "new-refresh-token"));

        var result = mvc.post().uri("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refresh_token", "old-refresh-token"))
            .content("{\"deviceId\": \"device-1\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.data.accessToken").isEqualTo("access-token");
        assertThat(result).bodyJson().extractingPath("$.data.refreshToken").isNull();
        verify(cookieManagerPort).setCookie(
            any(HttpServletResponse.class), eq("refresh_token"), eq("new-refresh-token"), eq(259200L));
    }

    @Test
    void logout_should_revoke_session_and_clear_cookie() {
        var result = mvc.post().uri("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refresh_token", "old-refresh-token"))
            .content("{\"deviceId\": \"device-1\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Đăng xuất thành công");
        verify(logoutUseCase).execute(new LogoutCommand("old-refresh-token", "device-1"));
        verify(cookieManagerPort).clearCookie(any(HttpServletResponse.class), eq("refresh_token"));
    }

    /**
     * Logout without the cookie must still answer 200 and clear the cookie: the client discards its
     * tokens right after this call, so a failure here would leave a live server session behind.
     */
    @Test
    void logout_should_return_ok_and_clear_cookie_when_refresh_cookie_is_missing() {
        var result = mvc.post().uri("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\": \"device-1\"}")
            .exchange();

        assertThat(result).hasStatusOk();
        verify(logoutUseCase).execute(new LogoutCommand(null, "device-1"));
        verify(cookieManagerPort).clearCookie(any(HttpServletResponse.class), eq("refresh_token"));
    }

    @Test
    void send_reset_password_otp_should_return_ok_response() {
        var result = mvc.post().uri("/api/v1/auth/reset-password-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"admin@example.com\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Mã OTP đặt lại mật khẩu đã được gửi");
        verify(sendResetPasswordOtpUseCase).execute(new SendResetPasswordOtpCommand("admin@example.com"));
    }

    @Test
    void send_reset_password_otp_should_map_not_found_exception_to_404() {
        when(sendResetPasswordOtpUseCase.execute(any(SendResetPasswordOtpCommand.class)))
            .thenThrow(new NotFoundException("User not found"));

        var result = mvc.post().uri("/api/v1/auth/reset-password-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"missing@example.com\"}")
            .exchange();

        assertThat(result).hasStatus(404).bodyJson().extractingPath("$.error").isEqualTo("NOT_FOUND");
    }

    @Test
    void reset_password_should_return_ok_response() {
        var result = mvc.patch().uri("/api/v1/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"admin@example.com\", \"password\": \"new-password\", \"otp\": \"1234567\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Mật khẩu đã thay đổi thành công");
        verify(resetPasswordUseCase)
            .execute(new ResetPasswordCommand("admin@example.com", "new-password", "1234567"));
    }
}
