package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.LoginCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank(message = "Login name is required") String login,

        @NotBlank(message = "Password is required") String password, 

        @Valid
        @NotNull(message = "Device information is required")
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
