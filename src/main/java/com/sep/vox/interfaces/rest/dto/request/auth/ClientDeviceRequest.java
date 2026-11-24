package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.ClientDeviceCommand;
import com.sep.vox.domain.model.devicesession.SessionPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientDeviceRequest(
    @NotBlank(message = "ID của thiết bị không được để trống")
    @Size(max = 255, message = "ID của thiết bị không được vuợt quá 255 ký tự")
    String deviceId, 
    
    @NotBlank(message = "Tên thiết bị không được để trống")
    @Size(max = 255, message = "Tên thiết bị không được vuợt quá 255 ký tự")
    String deviceName,

    @NotNull(message = "Nền tảng của thiết bị không được để trống")
    SessionPlatform platform
) {

    public static ClientDeviceCommand toCommand(ClientDeviceRequest request) {
        return new ClientDeviceCommand(
            request.deviceId(), 
            request.deviceName(), 
            request.platform()
        );
    }
}
