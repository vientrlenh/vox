package com.sep.vox.application.mapper.auth;

import com.sep.vox.application.response.input.auth.RefreshResponse;

public final class RefreshResponseMapper {
    
    public static RefreshResponse toResponse(String accessToken, String refreshToken) {
        return new RefreshResponse(accessToken, refreshToken);
    }
}
