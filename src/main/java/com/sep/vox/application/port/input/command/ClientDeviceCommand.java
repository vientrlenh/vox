package com.sep.vox.application.port.input.command;

public record ClientDeviceCommand(
    String deviceId,
    String deviceName,
    String platform
) {
    
}
