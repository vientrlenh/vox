package com.sep.vox.application.port.input.query.scoringrule;

import java.util.UUID;

public record SearchSchoolScoringRuleQuery(
        UUID schoolId,
        UUID policyId,
        String keyword,
        Boolean isActive,
        int page,
        int size
) {
}