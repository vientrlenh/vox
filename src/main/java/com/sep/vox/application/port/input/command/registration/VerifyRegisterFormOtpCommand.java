package com.sep.vox.application.port.input.command.registration;

public record VerifyRegisterFormOtpCommand(
    String email, 
    String otp
) {
    
}
