package com.sep.vox.application.port.input.command;

public record ResetPasswordCommand(
    String email,
    String password,
    String otp
) {
    
}
