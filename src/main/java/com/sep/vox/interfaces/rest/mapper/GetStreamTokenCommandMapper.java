package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.GetStreamTokenCommand;
import com.sep.vox.interfaces.rest.dto.request.GetStreamTokenRequest;

public final class GetStreamTokenCommandMapper {
    
    public static GetStreamTokenCommand fromRequest(GetStreamTokenRequest request) {
        return new GetStreamTokenCommand(
            request.scheduleIds(), 
            request.examSessionId(),
            request.examId(),
            request.streamTypes()
        );
    }
}
