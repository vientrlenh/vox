package com.sep.vox.application.response.output;

public record GeneratedSessionToken(
    String rawToken,
    String hashedToken
) {
    
}
