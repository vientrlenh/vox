package com.sep.vox.application.port.input.command;

public record RegisterPushTokenCommand(
    String deviceId,
    String pushToken
) {
}
