package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateAssessmentAppealCommand(
    UUID candidateResultId,
    List<UUID> paperItemIds,
    String reason,
    String notes
) {
}
