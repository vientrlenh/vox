package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.SetUpPasswordCommand;
import com.sep.vox.interfaces.rest.dto.request.SetUpPasswordRequest;

public final class SetUpPasswordCommandMapper {
    
    public static SetUpPasswordCommand fromRequest(SetUpPasswordRequest request) {
        return new SetUpPasswordCommand(
            request.userId(),
            request.token(),
            request.password()
        );
    }
}
