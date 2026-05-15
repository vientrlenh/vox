package com.sep.vox.application.mapper;

import com.sep.vox.application.response.LoginResponse;

public class LoginResponseMapper {
    
    public static LoginResponse toResponse(String accessToken) {
        return new LoginResponse(accessToken);
    }
}
