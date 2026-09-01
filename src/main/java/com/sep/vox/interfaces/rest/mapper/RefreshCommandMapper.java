package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.RefreshCommand;
import com.sep.vox.interfaces.rest.dto.request.DeviceIdRequest;

public final class RefreshCommandMapper {
    
    public static RefreshCommand fromRequest(DeviceIdRequest request, String token) {
        return new RefreshCommand(
            token, 
            request.deviceId()
        );
    }
}
