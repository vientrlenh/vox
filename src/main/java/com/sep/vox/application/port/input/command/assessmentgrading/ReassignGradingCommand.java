package com.sep.vox.application.port.input.command.assessmentgrading;

import java.util.UUID;

public record ReassignGradingCommand(
    UUID assignmentId,
    UUID teacherId
) {
}
