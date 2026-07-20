package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record IssueMonitorTokenRequest(
    List<UUID> sessionIds, 

    List<UUID> scheduleIds, 

    @NotNull(message = "Id của kỳ thi không được để trống")
    UUID examId
) {
    
}
