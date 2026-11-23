package com.sep.vox.interfaces.rest.dto.request.registration;

import com.sep.vox.application.port.input.command.registration.VerifyRegisterFormOtpCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyRegisterFormOtpRequest(
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    String email, 

    @NotBlank(message = "Mã xác thực không được để trống")
    String otp
) {

    public static VerifyRegisterFormOtpCommand toCommand(VerifyRegisterFormOtpRequest request) {
        return new VerifyRegisterFormOtpCommand(request.email(), request.otp());
    }
}
