package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ArchiveSchoolAssessmentPolicyCommand(
        UUID schoolId,
        UUID policyId
) {
}