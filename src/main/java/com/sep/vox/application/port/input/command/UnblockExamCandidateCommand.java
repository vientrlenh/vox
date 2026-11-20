package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UnblockExamCandidateCommand(
    UUID candidateId,
    String reason
) {
}
