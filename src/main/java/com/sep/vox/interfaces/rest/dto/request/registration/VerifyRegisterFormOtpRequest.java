package com.sep.vox.interfaces.rest.dto.request.registration;

import com.sep.vox.application.port.input.command.registration.VerifyRegisterFormOtpCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyRegisterFormOtpRequest(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format is invalid")
    String email, 

    @NotBlank(message = "Verification code must not be blank")
    String otp
) {

    public static VerifyRegisterFormOtpCommand toCommand(VerifyRegisterFormOtpRequest request) {
        return new VerifyRegisterFormOtpCommand(request.email(), request.otp());
    }
}
