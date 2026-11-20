package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

public record ApproveExamAppealCommand(
    UUID appealId,
    Instant deadline
) {
}
