package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendResetPasswordOtpRequest (
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Email(message = "Invalid email address")
    String email
) {
    
    public static SendResetPasswordOtpCommand toCommand(SendResetPasswordOtpRequest request) {
        return new SendResetPasswordOtpCommand(request.email);
    }
}
