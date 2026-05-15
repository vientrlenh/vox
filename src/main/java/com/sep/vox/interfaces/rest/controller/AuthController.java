package com.sep.vox.interfaces.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.auth.LoginUseCase;
import com.sep.vox.application.response.LoginResponse;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;
import com.sep.vox.interfaces.rest.mapper.LoginCommandMapper;


import jakarta.validation.Valid;



@RestController
@RequestMapping("/api")
public class AuthController {
    
    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/v1/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        var command = LoginCommandMapper.fromRequest(request);
        var data = loginUseCase.execute(command);
        var response = new ApiResponse<>("Đăng nhập thành công", data);
        return ResponseEntity.ok(response);
    }
}
