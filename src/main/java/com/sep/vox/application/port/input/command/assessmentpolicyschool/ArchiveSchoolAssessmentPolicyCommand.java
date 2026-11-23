package com.sep.vox.application.port.input.command.assessmentpolicyschool;

import java.util.UUID;

public record ArchiveSchoolAssessmentPolicyCommand(
        UUID schoolId,
        UUID policyId
) {
}