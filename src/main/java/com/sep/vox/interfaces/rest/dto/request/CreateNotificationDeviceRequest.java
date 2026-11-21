package com.sep.vox.interfaces.rest.dto.request;

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
    
}
