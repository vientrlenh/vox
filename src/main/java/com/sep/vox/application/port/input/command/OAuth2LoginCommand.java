package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.platform.Platform;

public record OAuth2LoginCommand(
    String provider,
    String providerUserId,
    String email,
    Boolean emailVerified,
    String fullName,
    String avatarUrl,
    String deviceId, 
    String deviceName, 
    Platform platform, 
    String ipAddress, 
    String userAgent
) {
    
}
