package com.sep.vox.application.port.input.command.assessmentpolicyschool;

import java.util.UUID;

public record PublishSchoolAssessmentPolicyCommand(
        UUID schoolId,
        UUID policyId
) {
}
