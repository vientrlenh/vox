package com.sep.vox.application.port.input.command.assessmentappeal;

import java.time.Instant;
import java.util.UUID;

public record ApproveAssessmentAppealCommand(
    UUID appealId,
    Instant deadline
) {
}
