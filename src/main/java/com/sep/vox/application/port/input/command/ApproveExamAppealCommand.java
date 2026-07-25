package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApproveExamAppealCommand(
    UUID appealId,
    OffsetDateTime deadline
) {
}
