package com.sep.vox.interfaces.rest.dto.request.auth;

import com.sep.vox.application.port.input.command.auth.SetUpPasswordCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetUpPasswordRequest(
    @NotNull(message = "Id người dùng không được để trống")
    UUID userId,

    @NotBlank(message = "Token không được để trống")
    String token,

    @NotBlank(message = "Mật khẩu không được để trống")
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
