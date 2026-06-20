package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.VerifyRegisterFormOtpCommand;
import com.sep.vox.interfaces.rest.dto.request.VerifyRegisterFormOtpRequest;

public final class VerifyRegisterFormOtpCommandMapper {
    
    public static VerifyRegisterFormOtpCommand fromRequest(VerifyRegisterFormOtpRequest request) {
        return new VerifyRegisterFormOtpCommand(request.email(), request.otp());
    }
}
