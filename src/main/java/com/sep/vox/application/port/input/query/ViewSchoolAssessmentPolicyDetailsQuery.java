package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSchoolAssessmentPolicyDetailsQuery(
        UUID schoolId,
        UUID policyId
) {}
