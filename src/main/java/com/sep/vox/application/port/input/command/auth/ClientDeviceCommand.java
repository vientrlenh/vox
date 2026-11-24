package com.sep.vox.application.port.input.command.auth;

import com.sep.vox.domain.model.devicesession.SessionPlatform;

public record ClientDeviceCommand(
    String deviceId,
    String deviceName,
    SessionPlatform platform
) {
    
}
