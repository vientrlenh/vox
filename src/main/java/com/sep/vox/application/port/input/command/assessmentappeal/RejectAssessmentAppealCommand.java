package com.sep.vox.application.port.input.command.assessmentappeal;

import java.util.UUID;

public record RejectAssessmentAppealCommand(
    UUID appealId,
    String reason
) {
}
