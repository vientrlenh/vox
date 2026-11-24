package com.sep.vox.application.port.input.command.assessmentsession;

import java.util.UUID;

public record RetryGradingAssessmentSessionCommand(
    UUID sessionId
) {
}
