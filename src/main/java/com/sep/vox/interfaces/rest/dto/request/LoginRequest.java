package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.domain.model.platform.Platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email/phone number is required") 
        String login,

        @NotBlank(message = "Password is required") 
        String password, 

        @NotBlank(message = "Device ID is required")
        @Size(max = 255, message = "Device ID must not exceed 255 characters")
        String deviceId, 
        
        @NotBlank(message = "Device name is required")
        @Size(max = 255, message = "Device name must not exceed 255 characters")
        String deviceName,

        @NotNull(message = "Device platform is required")
        Platform platform
) {
        public static LoginCommand toCommand(LoginRequest request, HttpServletRequest req, HttpServletResponse res) {
                return new LoginCommand(
                        request.login(), 
                        request.password(),
                        request.deviceId(), 
                        request.deviceName(), 
                        request.platform(), 
                        req, 
                        res
                );      
        }
}

