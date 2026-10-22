package com.sep.vox.application.response.input.auth;

public record LoginResponse(
    String email,
    String accessToken, 
    String refreshToken, 
    long refreshTokenExpirationMs
) {
    public static LoginResponse toResponse(String email, String accessToken, String refreshToken, long refreshTokenExpirationMs) {
        return new LoginResponse(email, accessToken, refreshToken, refreshTokenExpirationMs);
    }
}
