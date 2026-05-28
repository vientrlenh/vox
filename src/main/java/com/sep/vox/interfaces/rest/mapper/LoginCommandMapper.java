package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.LoginCommand;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;

public final class LoginCommandMapper {
    
    public static LoginCommand fromRequest(LoginRequest request, String ipAddress, String userAgent) {
        return new LoginCommand(
            request.login(), 
            request.password(),
            ipAddress, 
            userAgent,
            ClientDeviceCommandMapper.fromRequest(request.device())
        );
    }
}
