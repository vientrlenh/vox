package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
import com.sep.vox.application.port.input.command.RegisterBySelfDeclaredCommand;
import com.sep.vox.application.port.input.command.RegisterFromSchoolDirectoryCommand;
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
import com.sep.vox.interfaces.shared.HttpCookieProvider;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.input.registration.RegisterFromSchoolDirectoryResponse;
import com.sep.vox.support.RestControllerTest;
import com.sep.vox.domain.model.devicesession.SessionPlatform;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

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
    private HttpCookieProvider httpCookieProvider;

    @MockitoBean
    private RegisterFromSchoolDirectoryUseCase registerFromSchoolDirectoryUseCase;
    @MockitoBean
    private RegisterBySelfDeclaredUseCase registerBySelfDeclaredUseCase;

    // Required by the AuthController constructor; no test in this class calls them.
    @MockitoBean
    private SetUpPasswordUseCase setUpPasswordUseCase;
    @MockitoBean
    private VerifyRegisterFormOtpUseCase verifyRegisterFormOtpUseCase;
    @MockitoBean
    private GoogleTokenLoginUseCase googleTokenLoginUseCase;

    @Test
    void login_should_return_tokens_without_refresh_token_and_set_cookie() {
        ClientDeviceCommand device = new ClientDeviceCommand("device-1", "Chrome on Windows", SessionPlatform.WEB);
        LoginCommand expectedCommand = new LoginCommand(
            "admin@example.com", "password", "203.0.113.10", "JUnit User Agent", device);
        when(loginUseCase.execute(expectedCommand))
            .thenReturn(new LoginResponse("access-token", "refresh-token", List.of("STUDENT")));

        MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Forwarded-For", "203.0.113.10")
            .header("User-Agent", "JUnit User Agent")
            .content(LOGIN_JSON)
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.data.accessToken").isEqualTo("access-token");
        assertThat(result).bodyJson().extractingPath("$.data.refreshToken").isNull();
        verify(httpCookieProvider).setCookie(
            any(HttpServletResponse.class), eq("refresh_token"), eq("refresh-token"), eq(259200L));
    }

    @Test
    void login_should_return_bad_request_and_skip_use_case_when_body_is_invalid() {
        MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\": \"\", \"password\": \"password\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.message").isEqualTo("BAD_REQUEST");
        verifyNoInteractions(loginUseCase, httpCookieProvider);
    }

    @Test
    void login_should_accept_a_platform_in_any_letter_case() {
        ClientDeviceCommand device = new ClientDeviceCommand("device-1", "Chrome on Windows", SessionPlatform.WEB);
        when(loginUseCase.execute(any(LoginCommand.class)))
            .thenReturn(new LoginResponse("access-token", "refresh-token", List.of("STUDENT")));

        MvcTestResult result = loginWithPlatform("\"web\"");

        assertThat(result).hasStatusOk();
        verify(loginUseCase).execute(argThat(command -> device.equals(command.device())));
    }

    @Test
    void login_should_return_400_naming_the_field_for_an_unknown_platform() {
        MvcTestResult result = loginWithPlatform("\"PLAYSTATION\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors").isNotNull();
        verifyNoInteractions(loginUseCase, httpCookieProvider);
    }

    @Test
    void login_should_return_400_when_the_platform_is_missing() {
        MvcTestResult result = loginWithPlatform("null");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors").isNotNull();
        verifyNoInteractions(loginUseCase, httpCookieProvider);
    }

    @Test
    void google_oauth_start_should_return_400_for_an_unknown_platform() {
        MvcTestResult result = mvc.get().uri("/api/v1/auth/oauth2/google/start")
            .param("deviceId", "device-1").param("deviceName", "Chrome").param("platform", "PLAYSTATION")
            .exchange();

        assertThat(result).hasStatus(400);
    }

    private MvcTestResult loginWithPlatform(String platformJson) {
        String body = "{\"login\": \"admin@example.com\", \"password\": \"password\", \"device\": "
            + "{\"deviceId\": \"device-1\", \"deviceName\": \"Chrome on Windows\", \"platform\": " + platformJson + "}}";
        return mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    void login_should_map_unauthorized_exception_to_401_error_body() {
        when(loginUseCase.execute(any(LoginCommand.class)))
            .thenThrow(new UnauthorizedException("Invalid credentials"));

        MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(LOGIN_JSON)
            .exchange();

        assertThat(result).hasStatus(401).bodyJson().extractingPath("$.error").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(httpCookieProvider);
    }

    @Test
    void refresh_should_rotate_refresh_cookie_and_hide_new_token_from_body() {
        when(refreshUseCase.execute(new RefreshCommand("old-refresh-token", "device-1")))
            .thenReturn(new RefreshResponse("access-token", "new-refresh-token"));

        MvcTestResult result = mvc.post().uri("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refresh_token", "old-refresh-token"))
            .content("{\"deviceId\": \"device-1\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.data.accessToken").isEqualTo("access-token");
        assertThat(result).bodyJson().extractingPath("$.data.refreshToken").isNull();
        verify(httpCookieProvider).setCookie(
            any(HttpServletResponse.class), eq("refresh_token"), eq("new-refresh-token"), eq(259200L));
    }

    @Test
    void logout_should_revoke_session_and_clear_cookie() {
        MvcTestResult result = mvc.post().uri("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refresh_token", "old-refresh-token"))
            .content("{\"deviceId\": \"device-1\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Đăng xuất thành công");
        verify(logoutUseCase).execute(new LogoutCommand("old-refresh-token", "device-1"));
        verify(httpCookieProvider).clearCookie(any(HttpServletResponse.class), eq("refresh_token"));
    }

    /**
     * Logout without the cookie must still answer 200 and clear the cookie: the client discards its
     * tokens right after this call, so a failure here would leave a live server session behind.
     */
    @Test
    void logout_should_return_ok_and_clear_cookie_when_refresh_cookie_is_missing() {
        MvcTestResult result = mvc.post().uri("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\": \"device-1\"}")
            .exchange();

        assertThat(result).hasStatusOk();
        verify(logoutUseCase).execute(new LogoutCommand(null, "device-1"));
        verify(httpCookieProvider).clearCookie(any(HttpServletResponse.class), eq("refresh_token"));
    }

    @Test
    void send_reset_password_otp_should_return_ok_response() {
        MvcTestResult result = mvc.post().uri("/api/v1/auth/reset-password-otp")
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

        MvcTestResult result = mvc.post().uri("/api/v1/auth/reset-password-otp")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"missing@example.com\"}")
            .exchange();

        assertThat(result).hasStatus(404).bodyJson().extractingPath("$.error").isEqualTo("NOT_FOUND");
    }

    @Test
    void reset_password_should_return_ok_response() {
        MvcTestResult result = mvc.patch().uri("/api/v1/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"admin@example.com\", \"password\": \"new-password\", \"otp\": \"1234567\"}")
            .exchange();

        assertThat(result).hasStatusOk().bodyJson()
            .extractingPath("$.message").isEqualTo("Mật khẩu đã thay đổi thành công");
        verify(resetPasswordUseCase)
            .execute(new ResetPasswordCommand("admin@example.com", "new-password", "1234567"));
    }

    private static final String DIRECTORY_REGISTRATION_JSON = """
        {"schoolDirectoryId": "11111111-1111-1111-1111-111111111111", "contactFullName": "Nguyen Van A",
         "identityNumber": "079123456789", "contactPhone": "0987654321", "contactEmail": "a@school.edu.vn",
         "contactAddress": "1 Street", "postalCode": "70000", "position": "Principal", "studentCount": 100%s}""";

    private static final String SELF_DECLARED_REGISTRATION_JSON = """
        {"schoolName": "School 1", "schoolDistrict": "District 1", "schoolProvince": "HCM",
         "schoolAddress": "1 Street", "contactFullName": "Nguyen Van A", "identityNumber": "079123456789",
         "contactPhone": "0987654321", "contactEmail": "a@school.edu.vn", "contactAddress": "1 Street",
         "postalCode": "70000", "position": "Principal", "studentCount": 100, "documentUrls": []%s}""";

    private MvcTestResult register(String path, String template, String dateOfBirthJson) {
        return mvc.post().uri("/api/v1/auth/" + path).contentType(MediaType.APPLICATION_JSON)
            .content(template.formatted(dateOfBirthJson)).exchange();
    }

    @Test
    void register_school_directory_should_pass_iso_date_of_birth_to_the_command() {
        RegisterFromSchoolDirectoryResponse expected = new RegisterFromSchoolDirectoryResponse("sent");
        when(registerFromSchoolDirectoryUseCase.execute(any(RegisterFromSchoolDirectoryCommand.class)))
            .thenReturn(expected);

        MvcTestResult result = register("register-school-directory", DIRECTORY_REGISTRATION_JSON,
            ", \"dateOfBirth\": \"1980-05-20\"");

        assertThat(result).hasStatus(201);
        verify(registerFromSchoolDirectoryUseCase).execute(new RegisterFromSchoolDirectoryCommand(
            UUID.fromString("11111111-1111-1111-1111-111111111111"), "Nguyen Van A", "079123456789",
            "0987654321", "a@school.edu.vn", LocalDate.of(1980, 5, 20), "1 Street", "70000", "Principal", 100, null));
    }

    @Test
    void register_school_directory_should_return_400_naming_the_field_for_a_bad_date_of_birth() {
        MvcTestResult result = register("register-school-directory", DIRECTORY_REGISTRATION_JSON,
            ", \"dateOfBirth\": \"20/05/1980\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.dateOfBirth").isNotNull();
        verifyNoInteractions(registerFromSchoolDirectoryUseCase);
    }

    @Test
    void register_school_directory_should_return_400_when_date_of_birth_is_missing() {
        MvcTestResult result = register("register-school-directory", DIRECTORY_REGISTRATION_JSON, "");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.dateOfBirth").isNotNull();
        verifyNoInteractions(registerFromSchoolDirectoryUseCase);
    }

    @Test
    void register_self_declared_should_pass_iso_date_of_birth_to_the_command() {
        MvcTestResult result = register("register-self-declared", SELF_DECLARED_REGISTRATION_JSON,
            ", \"dateOfBirth\": \"1980-05-20\"");

        assertThat(result).hasStatus(201);
        verify(registerBySelfDeclaredUseCase).execute(new RegisterBySelfDeclaredCommand("School 1", null,
            "District 1", "HCM", "1 Street", "Nguyen Van A", "079123456789", "0987654321", "a@school.edu.vn",
            LocalDate.of(1980, 5, 20), "1 Street", "70000", "Principal", 100, List.of()));
    }

    @Test
    void register_self_declared_should_return_400_naming_the_field_for_a_bad_date_of_birth() {
        MvcTestResult result = register("register-self-declared", SELF_DECLARED_REGISTRATION_JSON,
            ", \"dateOfBirth\": \"31/02/1980\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.dateOfBirth").isNotNull();
        verifyNoInteractions(registerBySelfDeclaredUseCase);
    }

    @Test
    void register_self_declared_should_return_400_when_date_of_birth_is_missing() {
        MvcTestResult result = register("register-self-declared", SELF_DECLARED_REGISTRATION_JSON, "");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.dateOfBirth").isNotNull();
        verifyNoInteractions(registerBySelfDeclaredUseCase);
    }
}
