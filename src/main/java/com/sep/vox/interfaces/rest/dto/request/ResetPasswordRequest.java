package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.application.port.input.command.ResetPasswordCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    String email,

    @NotBlank(message = "Password is required")
    String password, 

    @NotBlank(message = "Password confirmation is required")
    String confirmPassword,

    @NotBlank(message = "Otp is required")
    String otp
) {
    
    public static ResetPasswordCommand toCommand(ResetPasswordRequest request) {
        return new ResetPasswordCommand(
            request.email, 
            request.password, 
            request.confirmPassword, 
            request.otp
        );
    }
}
