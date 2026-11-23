package com.sep.vox.application.port.input.query.scoringrule;

import java.util.UUID;

public record ViewScoringRuleDetailQuery(UUID policyId, UUID ruleId) {
}