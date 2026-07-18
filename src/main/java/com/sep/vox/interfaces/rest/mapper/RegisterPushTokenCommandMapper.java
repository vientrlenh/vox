package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.RegisterPushTokenCommand;
import com.sep.vox.interfaces.rest.dto.request.RegisterPushTokenRequest;

public final class RegisterPushTokenCommandMapper {

    private RegisterPushTokenCommandMapper() {}

    public static RegisterPushTokenCommand fromRequest(RegisterPushTokenRequest request) {
        return new RegisterPushTokenCommand(request.deviceId(), request.pushToken());
    }
}
