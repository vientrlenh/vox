package com.sep.vox.application.response.input.auth;

public record RefreshResponse(
    String accessToken, 
    String refreshToken, 
    long refreshTokenExpirationMs
) {
    public static RefreshResponse toResponse(String accessToken, String refreshToken, long refreshTokenExpirationMs) {
        return new RefreshResponse(accessToken, refreshToken, refreshTokenExpirationMs);
    }
}
