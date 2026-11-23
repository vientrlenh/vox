package com.sep.vox.application.port.input.command.auth;

public record LoginCommand(
    String login,
    String password,
    String ipAddress,
    String userAgent,
    ClientDeviceCommand device
) {
    
}
