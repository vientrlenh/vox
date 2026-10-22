package com.sep.vox.application.port.input.command;

public record RefreshCommand(
    String deviceId, 
    String refreshToken
) {
    
}
