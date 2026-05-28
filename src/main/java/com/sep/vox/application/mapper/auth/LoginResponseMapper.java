package com.sep.vox.application.mapper.auth;

import java.util.List;

import com.sep.vox.application.response.input.auth.LoginResponse;

public class LoginResponseMapper {
    
    public static LoginResponse toResponse(String accessToken, String refreshToken, List<String> roles) {
        return new LoginResponse(accessToken, refreshToken, roles);
    }
}
