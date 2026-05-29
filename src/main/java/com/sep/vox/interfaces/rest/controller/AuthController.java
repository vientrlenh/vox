package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.RefreshUseCase;
import com.sep.vox.application.port.input.usecase.auth.RegisterUseCase;
import com.sep.vox.application.port.input.usecase.auth.SetUpPasswordUseCase;
import com.sep.vox.application.response.input.auth.LoginResponse;
import com.sep.vox.application.response.input.auth.RefreshResponse;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.RefreshRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterRequest;
import com.sep.vox.interfaces.rest.dto.request.SetUpPasswordRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.LoginCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RefreshCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RegisterCommandMapper;
import com.sep.vox.interfaces.rest.mapper.SetUpPasswordCommandMapper;
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

    public AuthController(LoginUseCase loginUseCase, RegisterUseCase registerUseCase, SetUpPasswordUseCase setUpPasswordUseCase, RefreshUseCase refreshUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.setUpPasswordUseCase = setUpPasswordUseCase;
        this.refreshUseCase = refreshUseCase;
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
}
