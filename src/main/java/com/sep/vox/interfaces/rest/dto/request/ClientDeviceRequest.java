package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientDeviceRequest(
    @NotBlank(message = "ID của thiết bị không được để trống")
    @Size(max = 255, message = "ID của thiết bị không được vuợt quá 255 ký tự")
    String deviceId, 
    
    @NotBlank(message = "Tên thiết bị không được để trống")
    @Size(max = 255, message = "Tên thiết bị không được vuợt quá 255 ký tự")
    String deviceName,

    @NotBlank(message = "Nền tảng của thiết bị không được để trống")
    @Size(max = 20, message = "Nền tảng của thiết bị không được vuợt quá 20 ký tự")
    String platform,

    @Size(max = 255, message = "Push token không được vuợt quá 255 ký tự")
    String pushToken
) {
    
}
