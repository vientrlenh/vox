package com.sep.vox.application.response.input.auth;

public record LoginResponse(
    String email,
    String accessToken
) {
    public static LoginResponse toResponse(String email, String accessToken) {
        return new LoginResponse(email, accessToken);
    }
}
