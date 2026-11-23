package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.ResetPasswordCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    String email,

    @NotBlank(message = "Mật khẩu không được để trống")
    String password, 

    @NotBlank(message = "Otp không được để trống")
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
