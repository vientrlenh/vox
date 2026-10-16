package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateAssessmentSessionCommand(
    UUID assessmentId,
    UUID candidateId,
    UUID paperId
) {
}
