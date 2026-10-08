package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.application.port.input.command.RefreshCommand;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    String deviceId
) {
    
    public static RefreshCommand toCommand(RefreshRequest request, HttpServletRequest req, HttpServletResponse res) {
        return new RefreshCommand(
            request.deviceId, 
            req, 
            res
        );
    }
}
