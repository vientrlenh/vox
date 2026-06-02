package com.sep.vox.application.port.input.command;

public record OAuth2LoginCommand(
    String provider,
    String providerUserId,
    String email,
    Boolean emailVerified,
    String fullName,
    String avatarUrl,
    String ipAddress, 
    String userAgent, 
    ClientDeviceCommand device
) {
    
}
