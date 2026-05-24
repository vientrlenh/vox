package com.sep.vox.application.mapper.auth;

import com.sep.vox.application.response.auth.LoginResponse;

public class LoginResponseMapper {
    
    public static LoginResponse toResponse(String accessToken, String refreshToken) {
        return new LoginResponse(accessToken, refreshToken);
    }
}
