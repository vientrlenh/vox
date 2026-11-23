package com.sep.vox.application.port.input.command.auth;

public record RefreshCommand(
    String token,
    String deviceId
) {
    
}
