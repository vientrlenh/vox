package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.SendResetPasswordOtpCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendResetPasswordOtpRequest (
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    String email
) {

    public static SendResetPasswordOtpCommand toCommand(SendResetPasswordOtpRequest request) {
        return new SendResetPasswordOtpCommand(request.email());
    }
}
