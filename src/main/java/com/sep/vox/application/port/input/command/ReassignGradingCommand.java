package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ReassignGradingCommand(
    UUID assignmentId,
    UUID teacherId
) {
}
