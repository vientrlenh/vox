package com.sep.vox.application.response.output;

public record GeneratedPasswordSetUpToken(
    String rawToken,
    String hashedToken
) {
    
}
