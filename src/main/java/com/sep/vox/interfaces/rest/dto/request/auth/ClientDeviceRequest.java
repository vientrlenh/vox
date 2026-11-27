package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.ClientDeviceCommand;
import com.sep.vox.domain.model.devicesession.SessionPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientDeviceRequest(
    @NotBlank(message = "Device ID must not be blank")
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    String deviceId, 
    
    @NotBlank(message = "Device name must not be blank")
    @Size(max = 255, message = "Device name must not exceed 255 characters")
    String deviceName,

    @NotNull(message = "Device platform must not be blank")
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
