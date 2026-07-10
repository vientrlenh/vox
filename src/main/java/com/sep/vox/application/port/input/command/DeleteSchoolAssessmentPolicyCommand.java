package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolAssessmentPolicyCommand(
        UUID schoolId,
        UUID policyId
) {
}
