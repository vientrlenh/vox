package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;

public record UpdateAssessmentDeliveryModeCommand(
    UUID assessmentId,
    AssessmentDeliveryMode deliveryMode
) {
}
