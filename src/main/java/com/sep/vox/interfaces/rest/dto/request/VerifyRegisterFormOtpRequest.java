package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.application.port.input.command.VerifyRegisterFormOtpCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyRegisterFormOtpRequest(
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Email(message = "Invalid email address")
    String email, 

    @NotBlank(message = "Otp is required")
    String otp
) {
    
    public static VerifyRegisterFormOtpCommand toCommand(VerifyRegisterFormOtpRequest request) {
        return new VerifyRegisterFormOtpCommand(request.email, request.otp);
    }
}
