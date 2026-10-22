package com.sep.vox.application.port.input.command;

public record LogoutCommand(
    String deviceId, 
    String refreshToken
) {

}
