package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewAllSchoolScoringRuleQuery(UUID schoolId, UUID policyId, int page, int size) {
}