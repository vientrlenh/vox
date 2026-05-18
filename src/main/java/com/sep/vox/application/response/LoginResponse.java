package com.sep.vox.application.response;

public record LoginResponse(
    String accessToken,
    String refreshToken
) {
    
}
