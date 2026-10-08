package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.platform.Platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    HttpServletRequest req, 
    HttpServletResponse res
) {
    
}
