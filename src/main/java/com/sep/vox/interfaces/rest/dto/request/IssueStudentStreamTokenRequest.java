package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.application.port.input.command.IssueStudentStreamTokenCommand;

import jakarta.validation.constraints.NotNull;


public record IssueStudentStreamTokenRequest(
    @NotNull(message = "Assessment session ID is required")
    UUID assessmentSessionId,
    String streamType
) {
    public static IssueStudentStreamTokenCommand toCommand(IssueStudentStreamTokenRequest request) {
        return new IssueStudentStreamTokenCommand(request.assessmentSessionId, request.streamType);
    }
}
