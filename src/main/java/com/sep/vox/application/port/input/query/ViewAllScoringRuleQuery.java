package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewAllScoringRuleQuery(UUID policyId, int page, int size) {
}