package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.interfaces.rest.dto.request.DeviceIdRequest;

public final class LogoutCommandMapper {

    public static LogoutCommand fromRequest(DeviceIdRequest request, String refreshToken) {
        return new LogoutCommand(
            refreshToken,
            request.deviceId()
        );
    }
}
