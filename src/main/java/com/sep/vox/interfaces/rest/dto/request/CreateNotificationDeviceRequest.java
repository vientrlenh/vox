package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.application.port.input.command.CreateNotificationDeviceCommand;
import com.sep.vox.domain.model.platform.Platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationDeviceRequest(
    @NotBlank(message = "Device ID is required")
    String deviceId, 

    @NotNull(message = "Device platform is required")
    Platform platform, 

    @NotBlank(message = "FID không được để trống")
    String installationId
) {
    
    public static CreateNotificationDeviceCommand toCommand(CreateNotificationDeviceRequest request) {
        return new CreateNotificationDeviceCommand(
            request.deviceId, 
            request.platform, 
            request.installationId
        );
    }
}
