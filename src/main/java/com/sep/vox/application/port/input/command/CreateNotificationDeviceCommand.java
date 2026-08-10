package com.sep.vox.application.port.input.command;

public record CreateNotificationDeviceCommand(
    String deviceId, 
    String platform, 
    String installationId
) {
    
}
