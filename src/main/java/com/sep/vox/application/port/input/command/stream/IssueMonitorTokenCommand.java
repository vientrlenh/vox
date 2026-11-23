package com.sep.vox.application.port.input.command.stream;

import java.util.List;
import java.util.UUID;

public record IssueMonitorTokenCommand(
    UUID assessmentId,
    List<UUID> scheduleIds
) {
    
}
