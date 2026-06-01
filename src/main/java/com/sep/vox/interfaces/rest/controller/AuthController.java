package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.application.port.input.usecase.auth.*;
import com.sep.vox.interfaces.rest.dto.request.*;
import com.sep.vox.interfaces.rest.mapper.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.shared.IpAddressReceiver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final SetUpPasswordUseCase setUpPasswordUseCase;
    private final RefreshUseCase refreshUseCase;
    private final SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterUseCase registerUseCase, SetUpPasswordUseCase setUpPasswordUseCase, RefreshUseCase refreshUseCase, SendResetPasswordOtpUseCase sendResetPasswordOtpUseCase, ResetPasswordUseCase resetPasswordUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.setUpPasswordUseCase = setUpPasswordUseCase;
        this.refreshUseCase = refreshUseCase;
        this.sendResetPasswordOtpUseCase = sendResetPasswordOtpUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        var ipAddress = IpAddressReceiver.getClientIp(servletRequest);
        var userAgent = servletRequest.getHeader("User-Agent");

        var command = LoginCommandMapper.fromRequest(request, ipAddress, userAgent);
        var data = loginUseCase.execute(command);
        var response = ApiResponse.success("Đăng nhập thành công", data);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody RegisterRequest request) {
        var command = RegisterCommandMapper.fromRequest(request);
        registerUseCase.execute(command);
        var response = ApiResponse.success("Đơn đăng ký đã được gửi thành công");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/setup-password")
    public ResponseEntity<ApiResponse<Object>> setUpPassword(@Valid @RequestBody SetUpPasswordRequest request) {
        var command = SetUpPasswordCommandMapper.fromRequest(request);
        setUpPasswordUseCase.execute(command);
        var response = ApiResponse.success("Mật khẩu đã được thiết lập thành công");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        var command = RefreshCommandMapper.fromRequest(request);
        var data = refreshUseCase.execute(command);
        var response = ApiResponse.success("Yêu cầu thành công", data);
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
}
