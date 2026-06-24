package com.sep.vox.application.port.input.command;

public record VerifyRegisterFormOtpCommand(
    String email, 
    String otp
) {
    
}
