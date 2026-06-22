package com.sep.vox.interfaces.rest.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.RefreshUseCase;
import com.sep.vox.application.port.input.usecase.auth.SetUpPasswordUseCase;
import com.sep.vox.application.port.input.usecase.auth.SendResetPasswordOtpUseCase;
import com.sep.vox.application.port.input.usecase.auth.ResetPasswordUseCase;
import com.sep.vox.application.port.input.usecase.auth.SendResetPasswordOtpUseCase;
import com.sep.vox.application.port.input.usecase.auth.SetUpPasswordUseCase;
import com.sep.vox.application.port.input.usecase.registration.RegisterBySelfDeclaredUseCase;

import com.sep.vox.application.port.input.usecase.registration.RegisterFromSchoolDirectoryUseCase;
import com.sep.vox.application.port.input.usecase.registration.VerifyRegisterFormOtpUseCase;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.application.response.input.registration.RegisterFromSchoolDirectoryResponse;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.RefreshRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterBySelfDeclaredRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterFromSchoolDirectoryRequest;
import com.sep.vox.interfaces.rest.dto.request.ResetPasswordRequest;
import com.sep.vox.interfaces.rest.dto.request.SendResetPasswordOtpRequest;
import com.sep.vox.interfaces.rest.dto.request.SetUpPasswordRequest;

import com.sep.vox.interfaces.rest.dto.request.VerifyRegisterFormOtpRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.LoginCommandMapper;
import com.sep.vox.interfaces.rest.mapper.SetUpPasswordCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RefreshCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RegisterBySelfDeclaredCommandMapper;
import com.sep.vox.interfaces.rest.mapper.SendResetPasswordOtpCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RegisterFromSchoolDirectoryCommandMapper;
import com.sep.vox.interfaces.rest.mapper.ResetPasswordCommandMapper;
import com.sep.vox.interfaces.rest.mapper.SendResetPasswordOtpCommandMapper;
import com.sep.vox.interfaces.rest.mapper.SetUpPasswordCommandMapper;
import com.sep.vox.interfaces.rest.mapper.VerifyRegisterFormOtpCommandMapper;

import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.shared.HttpCookieProvider;
import com.sep.vox.interfaces.shared.IpAddressReceiver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final LoginUseCase loginUseCase;
    private final RegisterFromSchoolDirectoryUseCase registerFromSchoolDirectoryUseCase;
    private final SetUpPasswordUseCase setUpPasswordUseCase;
    private final RefreshUseCase refreshUseCase;
    private final SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final RegisterBySelfDeclaredUseCase registerBySelfDeclaredUseCase;
    private final VerifyRegisterFormOtpUseCase verifyRegisterFormOtpUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterFromSchoolDirectoryUseCase registerFromSchoolDirectoryUseCase, SetUpPasswordUseCase setUpPasswordUseCase, RefreshUseCase refreshUseCase, SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase, ResetPasswordUseCase resetPasswordUseCase, RegisterBySelfDeclaredUseCase registerBySelfDeclaredUseCase, VerifyRegisterFormOtpUseCase verifyRegisterFormOtpUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerFromSchoolDirectoryUseCase = registerFromSchoolDirectoryUseCase;
        this.setUpPasswordUseCase = setUpPasswordUseCase;
        this.refreshUseCase = refreshUseCase;
        this.sendResetPasswordOtpUseCase = sendResetPasswordOtpUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.registerBySelfDeclaredUseCase = registerBySelfDeclaredUseCase;
        this.verifyRegisterFormOtpUseCase = verifyRegisterFormOtpUseCase;
    }

    private static final String REFRESH_TOKEN_COOKIE_KEY = "refresh_token";
    private static final long REFRESH_TOKEN_COOKIE_TTL_SECONDS = 259200L;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        var ipAddress = IpAddressReceiver.getClientIp(servletRequest);
        var userAgent = servletRequest.getHeader("User-Agent");

        var command = LoginCommandMapper.fromRequest(request, ipAddress, userAgent);
        var data = loginUseCase.execute(command);
        HttpCookieProvider.setCookie(servletResponse, REFRESH_TOKEN_COOKIE_KEY, data.refreshToken(), REFRESH_TOKEN_COOKIE_TTL_SECONDS);

        var response = ApiResponse.success("Đăng nhập thành công", new LoginResponse(data.accessToken(), null, data.roles()));

        return ResponseEntity.ok(response);
    }


    @PostMapping("/register-school-directory")
    public ResponseEntity<ApiResponse<RegisterFromSchoolDirectoryResponse>> registerSchoolDirectory(@Valid @RequestBody RegisterFromSchoolDirectoryRequest request) {
        var command = RegisterFromSchoolDirectoryCommandMapper.fromRequest(request);
        var data = registerFromSchoolDirectoryUseCase.execute(command);
        var response = ApiResponse.success("Yêu cầu thành công", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register-self-declared")
    public ResponseEntity<ApiResponse<Object>> registerSelfDeclared(@Valid @RequestBody RegisterBySelfDeclaredRequest request) {
        var command = RegisterBySelfDeclaredCommandMapper.fromRequest(request);
        registerBySelfDeclaredUseCase.execute(command);
        var response = ApiResponse.success("Đơn đăng ký đã được gửi thành công. Vui lòng đợi quản trị viên phê duyệt");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/verify-registration")
    public ResponseEntity<ApiResponse<Object>> verifyRegistrationOtp(@Valid @RequestBody VerifyRegisterFormOtpRequest request) {
        var command = VerifyRegisterFormOtpCommandMapper.fromRequest(request);
        verifyRegisterFormOtpUseCase.execute(command);
        var response = ApiResponse.success("Đơn yêu cầu đã được xác thực");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/setup-password")
    public ResponseEntity<ApiResponse<Object>> setUpPassword(@Valid @RequestBody SetUpPasswordRequest request) {
        var command = SetUpPasswordCommandMapper.fromRequest(request);
        setUpPasswordUseCase.execute(command);
        var response = ApiResponse.success("Mật khẩu đã được thiết lập thành công");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request, @CookieValue(name = REFRESH_TOKEN_COOKIE_KEY, required = true) String token, HttpServletResponse servletResponse) {
        var command = RefreshCommandMapper.fromRequest(request, token);
        var data = refreshUseCase.execute(command);
        HttpCookieProvider.setCookie(servletResponse, REFRESH_TOKEN_COOKIE_KEY, data.refreshToken(), REFRESH_TOKEN_COOKIE_TTL_SECONDS);
        var response = ApiResponse.success("Yêu cầu thành công", new RefreshResponse(
            data.accessToken(), null
        ));
        return ResponseEntity.ok(response);
    }


    @PostMapping("/reset-password-otp")
    public ResponseEntity<ApiResponse<Object>> sendResetPasswordOtp(@Valid @RequestBody SendResetPasswordOtpRequest request) {
        var command = SendResetPasswordOtpCommandMapper.fromRequest(request);
        sendResetPasswordOtpUseCase.execute(command);
        var response = ApiResponse.success("Mã OTP đặt lại mật khẩu đã được gửi");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        var command = ResetPasswordCommandMapper.fromRequest(request);
        resetPasswordUseCase.execute(command);
        var response = ApiResponse.success("Mật khẩu đã thay đổi thành công");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/google/start")
    public void startGoogleLogin(
        @RequestParam(required = true) String deviceId,
        @RequestParam(required = true) String deviceName,
        @RequestParam(required = true) String platform,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        var session = request.getSession();
        session.setAttribute("oauth2_device_id", deviceId);
        session.setAttribute("oauth2_device_name", deviceName);
        session.setAttribute("oauth2_platform", platform);

        response.sendRedirect("/oauth2/authorization/google");
    }
}
