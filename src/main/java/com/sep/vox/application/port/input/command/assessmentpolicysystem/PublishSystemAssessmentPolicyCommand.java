package com.sep.vox.application.port.input.command.assessmentpolicysystem;

import java.util.UUID;

public record PublishSystemAssessmentPolicyCommand(
        UUID policyId
) {
}
