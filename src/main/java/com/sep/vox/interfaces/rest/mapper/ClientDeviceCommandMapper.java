package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.ClientDeviceCommand;
import com.sep.vox.interfaces.rest.dto.request.ClientDeviceRequest;

public final class ClientDeviceCommandMapper {
    
    public static ClientDeviceCommand fromRequest(ClientDeviceRequest request) {
        return new ClientDeviceCommand(
            request.deviceId(), 
            request.deviceName(), 
            request.platform()
        );
    }
}
