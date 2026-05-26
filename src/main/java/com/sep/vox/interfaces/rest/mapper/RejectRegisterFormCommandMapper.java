package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;
import com.sep.vox.interfaces.rest.dto.request.RejectRegisterFormRequest;

public final class RejectRegisterFormCommandMapper {
    
    public static RejectRegisterFormCommand fromRequest(UUID registerFormId, RejectRegisterFormRequest request) {
        return new RejectRegisterFormCommand(
            registerFormId,
            request.reason()
        );
    }
}
