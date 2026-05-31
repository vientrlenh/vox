package com.sep.vox.application.port.input.command;

public record LoginGoogleCommand(
        String idToken,
        String ipAddress,
        String userAgent,
        ClientDeviceCommand device
) {}