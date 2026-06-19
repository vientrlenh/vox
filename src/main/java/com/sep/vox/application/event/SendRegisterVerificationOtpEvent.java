package com.sep.vox.application.event;

public record SendRegisterVerificationOtpEvent(
    String to, 
    String otp
) {
    
}
