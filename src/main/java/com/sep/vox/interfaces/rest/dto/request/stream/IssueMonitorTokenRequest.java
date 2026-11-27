package com.sep.vox.interfaces.rest.dto.request.stream;

import com.sep.vox.application.port.input.command.stream.IssueMonitorTokenCommand;
import java.util.List;
import java.util.UUID;


import jakarta.validation.constraints.NotNull;

public record IssueMonitorTokenRequest(
    @NotNull(message = "Assessment ID is required")
    UUID assessmentId,

    @NotNull(message = "Schedule list is required")
    List<UUID> scheduleIds
) {

    public static IssueMonitorTokenCommand toCommand(IssueMonitorTokenRequest request) {
        return new IssueMonitorTokenCommand(
            request.assessmentId(), 
            request.scheduleIds()
        );
    }
}
