package com.sep.vox.interfaces.rest.mapper;

import com.sep.vox.application.port.input.command.IssueStudentStreamTokenCommand;
import com.sep.vox.interfaces.rest.dto.request.IssueStudentStreamTokenRequest;

public final class IssueStudentStreamTokenCommandMapper {
    
    public static IssueStudentStreamTokenCommand fromRequest(IssueStudentStreamTokenRequest request) {
        return new IssueStudentStreamTokenCommand(
            request.examSessionId(),
            request.streamType()
        );
    }
}
