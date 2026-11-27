package com.sep.vox.interfaces.rest.dto.request.notification;

import com.sep.vox.application.port.input.command.notification.CreateNotificationDeviceCommand;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationDeviceRequest(
    @NotBlank(message = "Device ID must not be blank")
    String deviceId, 

    @NotNull(message = "Device platform must not be blank")
    NotificationDevicePlatform platform, 

    @NotBlank(message = "FID must not be blank")
    String installationId
) {

    public static CreateNotificationDeviceCommand toCommand(CreateNotificationDeviceRequest request)  {
        return new CreateNotificationDeviceCommand(
            request.deviceId(), 
            request.platform(), 
            request.installationId()
        );
    }
}
