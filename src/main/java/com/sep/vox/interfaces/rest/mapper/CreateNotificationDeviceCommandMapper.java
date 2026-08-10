package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.CreateNotificationDeviceCommand;
import com.sep.vox.interfaces.rest.dto.request.CreateNotificationDeviceRequest;

public final class CreateNotificationDeviceCommandMapper {
    
    private CreateNotificationDeviceCommandMapper() {}

    public static CreateNotificationDeviceCommand fromRequest(CreateNotificationDeviceRequest request)  {
        return new CreateNotificationDeviceCommand(
            request.deviceId(), 
            request.platform(), 
            request.installationId()
        );
    }
}
