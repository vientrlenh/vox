package com.sep.vox.application.event;

public record SendResetPasswordOtpEvent(
    String to,
    String otp
) {

}
