package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.IssueMonitorTokenCommand;
import com.sep.vox.interfaces.rest.dto.request.IssueMonitorTokenRequest;

public final class IssueMonitorTokenCommandMapper {
    
    public static IssueMonitorTokenCommand fromRequest(IssueMonitorTokenRequest request) {
        return new IssueMonitorTokenCommand(
            request.sessionIds(), 
            request.scheduleIds(), 
            request.examId()
        );
    }
}
