package com.sep.vox.application.port.input.command;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record RefreshCommand(
    String deviceId, 
    HttpServletRequest req, 
    HttpServletResponse res
) {
    
}
