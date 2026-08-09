package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateNotificationDeviceRequest(
    @NotBlank(message = "Id của thiết bị không được để trống")
    String deviceId, 

    @NotBlank(message = "Nền tảng của thiết bị không được để trống")
    String platform, 

    @NotBlank(message = "FID không được để trống")
    String installationId
) {
    
}
