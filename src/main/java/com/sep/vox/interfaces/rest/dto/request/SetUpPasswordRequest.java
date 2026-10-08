package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.SetUpPasswordCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetUpPasswordRequest(
    @NotNull(message = "User ID is required")
    UUID userId,

    @NotBlank(message = "Token is required")
    String token,

    @NotBlank(message = "Password is required")
    String password
) {
   
    public static SetUpPasswordCommand toCommand(SetUpPasswordRequest request) {
        return new SetUpPasswordCommand(
            request.userId, 
            request.token, 
            request.password
        );
    }
}
