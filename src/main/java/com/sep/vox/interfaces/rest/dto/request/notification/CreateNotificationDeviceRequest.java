package com.sep.vox.interfaces.rest.dto.request.notification;

import com.sep.vox.application.port.input.command.notification.CreateNotificationDeviceCommand;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationDeviceRequest(
    @NotBlank(message = "Id của thiết bị không được để trống")
    String deviceId, 

    @NotNull(message = "Nền tảng của thiết bị không được để trống")
    NotificationDevicePlatform platform, 

    @NotBlank(message = "FID không được để trống")
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
