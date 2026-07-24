package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateExamAppealCommand(
    UUID candidateResultId,
    List<UUID> paperItemIds,
    String reason,
    String notes
) {
}
