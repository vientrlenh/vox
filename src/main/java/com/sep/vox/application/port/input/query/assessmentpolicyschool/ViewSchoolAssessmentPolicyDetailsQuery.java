package com.sep.vox.application.port.input.query.assessmentpolicyschool;

import java.util.UUID;

public record ViewSchoolAssessmentPolicyDetailsQuery(
        UUID schoolId,
        UUID policyId
) {}
