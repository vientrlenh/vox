package com.sep.vox.application.port.input.command.auth;

public record LogoutCommand(
    String refreshToken,
    String deviceId
) {

}
