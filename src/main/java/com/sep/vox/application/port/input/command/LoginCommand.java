package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.platform.Platform;

public record LoginCommand(
    String login,
    String password,
    String deviceId, 
    String deviceName, 
    Platform platform, 
    String ipAddress, 
    String userAgent
) {
    
}
