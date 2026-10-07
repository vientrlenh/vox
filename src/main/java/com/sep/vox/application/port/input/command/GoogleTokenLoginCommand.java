package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.platform.Platform;

public record GoogleTokenLoginCommand(
    String idToken,
    String ipAddress,
    String userAgent,
    String deviceId, 
    String deviceName, 
    Platform platform
) {
}
