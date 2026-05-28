package com.sep.vox.application.response.input.auth;

import java.util.List;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    List<String> roles
) {
    
}
