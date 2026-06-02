package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.application.port.input.command.RegisterCommand;
import com.sep.vox.application.port.input.command.ResetPasswordCommand;
import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.RefreshUseCase;
import com.sep.vox.application.port.input.usecase.auth.RegisterUseCase;
import com.sep.vox.application.port.input.usecase.auth.ResetPasswordUseCase;
import com.sep.vox.application.port.input.usecase.auth.SendResetPasswordOtpUseCase;
import com.sep.vox.application.port.input.usecase.auth.SetUpPasswordUseCase;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.interfaces.rest.dto.request.ClientDeviceRequest;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.RefreshRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterRequest;
import com.sep.vox.interfaces.rest.dto.request.ResetPasswordRequest;
import com.sep.vox.interfaces.rest.dto.request.SendResetPasswordOtpRequest;

import jakarta.servlet.http.HttpServletRequest;

public class AuthControllerTests {

    @Test
    void login_should_return_ok_response() {
        var loginUseCase = mock(LoginUseCase.class);
        var registerUseCase = mock(RegisterUseCase.class);
        var setUpPasswordUseCase = mock(SetUpPasswordUseCase.class);
        var servletRequest = mock(HttpServletRequest.class);
        var refreshUseCase = mock(RefreshUseCase.class);
        var sendResetPasswordOtpUseCase = mock(SendResetPasswordOtpUseCase.class);
        var resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        var controller = new AuthController(loginUseCase, registerUseCase, setUpPasswordUseCase, refreshUseCase, sendResetPasswordOtpUseCase, resetPasswordUseCase);
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

        var response = controller.login(request, servletRequest, servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new LoginResponse("access-token", null, roles));
        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
            .contains("refresh_token=refresh-token")
            .contains("HttpOnly")
            .contains("SameSite=Lax");
        verify(loginUseCase).execute(expectedCommand);
    }

    @Test
    void register_should_return_created_response() {
        var loginUseCase = mock(LoginUseCase.class);
        var registerUseCase = mock(RegisterUseCase.class);
        var setUpPasswordUseCase = mock(SetUpPasswordUseCase.class);
        var refreshUseCase = mock(RefreshUseCase.class);
        var sendResetPasswordOtpUseCase = mock(SendResetPasswordOtpUseCase.class);
        var resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        var controller = new AuthController(loginUseCase, registerUseCase, setUpPasswordUseCase, refreshUseCase, sendResetPasswordOtpUseCase, resetPasswordUseCase);
        var request = new RegisterRequest(
            "Nguyen Van A",
            "123456789",
            "0987654321",
            "admin@example.com",
            "2000-05-24",
            "123 Street",
            "school.edu.vn",
            "School Name",
            "456 School Street",
            "700000",
            "Principal",
            500
        );

        var expectedCommand = new RegisterCommand(
            request.contactFullName(),
            request.identityNumber(),
            request.contactPhone(),
            request.contactEmail(),
            java.time.LocalDate.of(2000, 5, 24),
            request.contactAddress(),
            request.schoolDomain(),
            request.schoolName(),
            request.schoolAddress(),
            request.postalCode(),
            request.position(),
            request.studentCount()
        );

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        verify(registerUseCase).execute(expectedCommand);
    }

    @Test
    void refresh_should_return_ok_response() {
        var loginUseCase = mock(LoginUseCase.class);
        var registerUseCase = mock(RegisterUseCase.class);
        var setUpPasswordUseCase = mock(SetUpPasswordUseCase.class);
        var refreshUseCase = mock(RefreshUseCase.class);
        var sendResetPasswordOtpUseCase = mock(SendResetPasswordOtpUseCase.class);
        var resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        var controller = new AuthController(loginUseCase, registerUseCase, setUpPasswordUseCase, refreshUseCase, sendResetPasswordOtpUseCase, resetPasswordUseCase);
        var request = new RefreshRequest("device-1");
        var expectedCommand = new RefreshCommand("old-refresh-token", request.deviceId());
        var refreshResponse = new RefreshResponse("access-token", "new-refresh-token");

        when(refreshUseCase.execute(expectedCommand))
            .thenReturn(refreshResponse);

        var servletResponse = new MockHttpServletResponse();

        var response = controller.refresh(request, "old-refresh-token", servletResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new RefreshResponse("access-token", null));
        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
            .contains("refresh_token=new-refresh-token")
            .contains("HttpOnly")
            .contains("SameSite=Lax");
        verify(refreshUseCase).execute(expectedCommand);
    }

    @Test
    void send_reset_password_otp_should_return_ok_response() {
        var loginUseCase = mock(LoginUseCase.class);
        var registerUseCase = mock(RegisterUseCase.class);
        var setUpPasswordUseCase = mock(SetUpPasswordUseCase.class);
        var refreshUseCase = mock(RefreshUseCase.class);
        var sendResetPasswordOtpUseCase = mock(SendResetPasswordOtpUseCase.class);
        var resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        var controller = new AuthController(loginUseCase, registerUseCase, setUpPasswordUseCase, refreshUseCase, sendResetPasswordOtpUseCase, resetPasswordUseCase);
        var request = new SendResetPasswordOtpRequest("admin@example.com");
        var expectedCommand = new SendResetPasswordOtpCommand(request.email());

        var response = controller.sendResetPasswordOtp(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Mã OTP đặt lại mật khẩu đã được gửi");
        assertThat(response.getBody().data()).isNull();
        verify(sendResetPasswordOtpUseCase).execute(expectedCommand);
    }

    @Test
    void reset_password_should_return_ok_response() {
        var loginUseCase = mock(LoginUseCase.class);
        var registerUseCase = mock(RegisterUseCase.class);
        var setUpPasswordUseCase = mock(SetUpPasswordUseCase.class);
        var refreshUseCase = mock(RefreshUseCase.class);
        var sendResetPasswordOtpUseCase = mock(SendResetPasswordOtpUseCase.class);
        var resetPasswordUseCase = mock(ResetPasswordUseCase.class);
        var controller = new AuthController(loginUseCase, registerUseCase, setUpPasswordUseCase, refreshUseCase, sendResetPasswordOtpUseCase, resetPasswordUseCase);
        var request = new ResetPasswordRequest("admin@example.com", "new-password", "1234567");
        var expectedCommand = new ResetPasswordCommand(request.email(), request.password(), request.otp());

        var response = controller.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Mật khẩu đã thay đổi thành công");
        assertThat(response.getBody().data()).isNull();
        verify(resetPasswordUseCase).execute(expectedCommand);
    }
}
