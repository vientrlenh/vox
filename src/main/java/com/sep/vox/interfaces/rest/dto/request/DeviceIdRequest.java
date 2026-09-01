package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceIdRequest(
    @NotBlank(message = "ID của thiết bị không được để trống")
    String deviceId
) {
    
}
