package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.LoginCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập là bắt buộc") String login,

        @NotBlank(message = "Mật khẩu là bắt buộc") String password, 

        @Valid
        @NotNull(message = "Thông tin của thiết bị là bắt buộc")
        ClientDeviceRequest device
) {

    public static LoginCommand toCommand(LoginRequest request, String ipAddress, String userAgent) {
        return new LoginCommand(
            request.login(), 
            request.password(),
            ipAddress, 
            userAgent,
            ClientDeviceRequest.toCommand(request.device())
        );
    }
}
