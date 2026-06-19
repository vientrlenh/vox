package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import com.sep.vox.application.port.input.command.ApproveRegisterFormCommand;
import com.sep.vox.interfaces.rest.dto.request.ApproveRegisterFormRequest;

public final class ApproveRegisterFormCommandMapper {
    
    public static ApproveRegisterFormCommand fromRequest(UUID registerFormId, ApproveRegisterFormRequest request) {
        return new ApproveRegisterFormCommand(
            registerFormId,
            request.schoolCode(), 
            request.description() 
        );
    }
}
