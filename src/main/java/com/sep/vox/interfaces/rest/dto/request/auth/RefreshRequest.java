package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.RefreshCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    String deviceId
) {
    
    public static RefreshCommand toCommand(RefreshRequest request, String refreshToken) {
        return new RefreshCommand(
            request.deviceId, 
            refreshToken
        );
    }
}
