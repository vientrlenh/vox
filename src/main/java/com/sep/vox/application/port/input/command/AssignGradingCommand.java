package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignGradingCommand(
    String roundType,
    Instant deadlineAt,
    List<AssignmentItemCommand> assignments
) {
    public record AssignmentItemCommand(
        UUID candidateResultId,
        UUID teacherId
    ) {
    }
}
