package com.sep.vox.application.response.input.auth;

public record RefreshResponse(
    String accessToken,
    String refreshToken
) {
    public static RefreshResponse toResponse(String accessToken, String refreshToken) {
        return new RefreshResponse(accessToken, refreshToken);
    }
}
