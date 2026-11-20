package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UnblockAssessmentCandidateCommand(
    UUID candidateId,
    String reason
) {
}
