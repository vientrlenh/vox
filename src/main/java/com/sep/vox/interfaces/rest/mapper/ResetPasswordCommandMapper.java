package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.ResetPasswordCommand;
import com.sep.vox.interfaces.rest.dto.request.ResetPasswordRequest;

public class ResetPasswordCommandMapper {
    
    public static ResetPasswordCommand fromRequest(ResetPasswordRequest request) {
        return new ResetPasswordCommand(
            request.email(), 
            request.password(), 
            request.otp()
        );
    }
}
