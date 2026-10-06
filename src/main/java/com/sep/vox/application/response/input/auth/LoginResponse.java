package com.sep.vox.application.response.input.auth;

public record LoginResponse(
    String email,
    String accessToken,
    String refreshToken
) {
    public static LoginResponse toResponse(String email, String accessToken, String refreshToken) {
        return new LoginResponse(email, accessToken, refreshToken);
    }
}
