package com.sep.vox.application.port.input.query.scoringrule;

import java.util.UUID;

// isActive = null means no filter on the enabled/disabled status (both are returned).
public record SearchScoringRuleQuery(
        UUID policyId,
        String keyword,
        Boolean isActive,
        int page,
        int size
) {
}