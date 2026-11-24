package com.sep.vox.application.port.input.command.assessmentsession;

import java.util.UUID;

public record FlagAssessmentSessionCommand(
    UUID sessionId,
    boolean flagged,
    String reason
) {
}
