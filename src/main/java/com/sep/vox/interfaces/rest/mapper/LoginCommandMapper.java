package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.command.LoginCommand;
import com.sep.vox.interfaces.rest.dto.request.LoginRequest;

public class LoginCommandMapper {
    
    public static LoginCommand fromRequest(LoginRequest request) {
        return new LoginCommand(request.login().trim(), request.password());
    }
}
