package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.SetUpPasswordCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetUpPasswordRequest(
    @NotNull(message = "User ID must not be blank")
    UUID userId,

    @NotBlank(message = "Token must not be blank")
    String token,

    @NotBlank(message = "Password must not be blank")
    String password
) {

    public static SetUpPasswordCommand toCommand(SetUpPasswordRequest request) {
        return new SetUpPasswordCommand(
            request.userId(),
            request.token(),
            request.password()
        );
    }
}
