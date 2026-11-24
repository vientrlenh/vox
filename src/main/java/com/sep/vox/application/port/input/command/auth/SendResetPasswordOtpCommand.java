package com.sep.vox.application.port.input.command.auth;

public record SendResetPasswordOtpCommand(
    String email
) {
    
}
