package com.sep.vox.application.port.input.query.scoringrule;

import java.util.UUID;

public record ViewSchoolScoringRuleDetailQuery(UUID schoolId, UUID policyId, UUID ruleId) {
}