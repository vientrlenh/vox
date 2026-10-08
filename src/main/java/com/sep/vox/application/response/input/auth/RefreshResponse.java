package com.sep.vox.application.response.input.auth;

public record RefreshResponse(
    String accessToken
) {
    public static RefreshResponse toResponse(String accessToken, String refreshToken) {
        return new RefreshResponse(accessToken);
    }
}
