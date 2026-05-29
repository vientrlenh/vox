package com.sep.vox.application.response.input.auth;

public record RefreshResponse(
    String accessToken,
    String refreshToken
) {
    
}
