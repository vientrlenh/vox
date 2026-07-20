package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record IssueMonitorTokenCommand(
    List<UUID> sessionIds, 
    List<UUID> scheduleIds, 
    UUID examId
) {
    
}
