package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateExamAppealCommand(
    UUID candidateResultId,
    UUID paperItemId,
    String reason,
    String notes
) {
}
