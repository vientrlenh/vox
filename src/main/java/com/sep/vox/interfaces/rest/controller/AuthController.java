package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.auth.LoginUseCase;
import com.sep.vox.application.port.input.usecase.auth.RegisterUseCase;
import com.sep.vox.application.port.input.usecase.systemadmin.RegisterSchoolAdminUseCase;
import com.sep.vox.application.response.auth.LoginResponse;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterRequest;
import com.sep.vox.interfaces.rest.dto.request.RegisterSchoolAdminRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.LoginCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RegisterCommandMapper;
import com.sep.vox.interfaces.rest.mapper.RegisterSchoolAdminCommandMapper;

import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RegisterSchoolAdminUseCase registerSchoolAdminUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterUseCase registerUseCase, RegisterSchoolAdminUseCase registerSchoolAdminUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.registerSchoolAdminUseCase = registerSchoolAdminUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        var command = LoginCommandMapper.fromRequest(request);
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

    @PostMapping("/register/schooladmin")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> registerSchoolAdmin(@Valid @RequestBody RegisterSchoolAdminRequest request) {
        var command = RegisterSchoolAdminCommandMapper.fromRequest(request);
        registerSchoolAdminUseCase.execute(command);
        var response = ApiResponse.success("Tài khoản quản trị nhà trường đã tạo thành công");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

}
