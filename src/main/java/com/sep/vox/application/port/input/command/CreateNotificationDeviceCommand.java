package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.platform.Platform;

public record CreateNotificationDeviceCommand(
    String deviceId, 
    Platform platform, 
    String installationId
) {
    
}
