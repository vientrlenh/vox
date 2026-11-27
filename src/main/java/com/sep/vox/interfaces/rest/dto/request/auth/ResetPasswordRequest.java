package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.ResetPasswordCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format is invalid")
    String email,

    @NotBlank(message = "Password must not be blank")
    String password, 

    @NotBlank(message = "Otp must not be blank")
    String otp
) {

    public static ResetPasswordCommand toCommand(ResetPasswordRequest request) {
        return new ResetPasswordCommand(
            request.email(), 
            request.password(), 
            request.otp()
        );
    }
}
