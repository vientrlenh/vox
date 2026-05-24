package com.sep.vox.application.response.auth;

public record LoginResponse(
    String accessToken,
    String refreshToken
) {
    
}
