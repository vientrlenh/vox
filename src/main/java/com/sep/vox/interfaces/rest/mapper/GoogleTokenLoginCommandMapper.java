package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.GoogleTokenLoginCommand;
import com.sep.vox.interfaces.rest.dto.request.GoogleTokenLoginRequest;

public final class GoogleTokenLoginCommandMapper {

    public static GoogleTokenLoginCommand fromRequest(GoogleTokenLoginRequest request, String ipAddress,
            String userAgent) {
        return new GoogleTokenLoginCommand(
            request.idToken(),
            ipAddress,
            userAgent,
            ClientDeviceCommandMapper.fromRequest(request.device())
        );
    }
}
