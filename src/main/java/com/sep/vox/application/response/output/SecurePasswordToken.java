package com.sep.vox.application.response.output;

public record SecurePasswordToken(
    String rawToken,
    String hashedToken
) {
    
}
