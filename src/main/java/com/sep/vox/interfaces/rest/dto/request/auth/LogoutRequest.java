package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.LogoutCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    String deviceId
) {
    
    public static LogoutCommand toCommand(LogoutRequest request, String refreshToken) {
        return new LogoutCommand(
            request.deviceId, 
            refreshToken
        );
    }
}
