package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.platform.Platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record LoginCommand(
    String login,
    String password,
    String deviceId, 
    String deviceName, 
    Platform platform, 
    HttpServletRequest req, 
    HttpServletResponse res
) {
    
}
