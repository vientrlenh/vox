package com.sep.vox.application.response.input.auth;

public record LoginResponse(
    String accessToken,
    String refreshToken
) {
    
}
