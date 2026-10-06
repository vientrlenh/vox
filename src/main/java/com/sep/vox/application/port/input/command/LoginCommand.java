package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.platform.Platform;

public record LoginCommand(
    String login,
    String password,
    String ipAddress,
    String userAgent,
    String deviceId, 
    String deviceName, 
    Platform platform
) {
    
}
