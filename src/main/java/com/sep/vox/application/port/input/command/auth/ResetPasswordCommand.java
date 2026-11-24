package com.sep.vox.application.port.input.command.auth;

public record ResetPasswordCommand(
    String email,
    String password,
    String otp
) {
    
}
