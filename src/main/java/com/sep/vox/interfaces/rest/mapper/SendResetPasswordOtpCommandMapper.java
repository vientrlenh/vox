package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.SendResetPasswordOtpCommand;
import com.sep.vox.interfaces.rest.dto.request.SendResetPasswordOtpRequest;

public final class SendResetPasswordOtpCommandMapper {
    

    public static SendResetPasswordOtpCommand fromRequest(SendResetPasswordOtpRequest request) {
        return new SendResetPasswordOtpCommand(request.email());
    }
}
