package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.LogoutCommand;
import com.sep.vox.application.port.input.command.auth.RefreshCommand;
import jakarta.validation.constraints.NotBlank;

public record DeviceIdRequest(
    @NotBlank(message = "Device ID must not be blank")
    String deviceId
) {

    public static LogoutCommand toLogoutCommand(DeviceIdRequest request, String refreshToken) {
        return new LogoutCommand(
            refreshToken,
            request.deviceId()
        );
    }

    public static RefreshCommand toRefreshCommand(DeviceIdRequest request, String token) {
        return new RefreshCommand(
            token, 
            request.deviceId()
        );
    }
}
