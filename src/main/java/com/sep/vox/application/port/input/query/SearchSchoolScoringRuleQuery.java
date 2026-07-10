package com.sep.vox.application.port.input.query;

import java.util.UUID;

// Truy vấn danh sách Scoring Rule của 1 Assessment Policy thuộc trường học.
public record SearchSchoolScoringRuleQuery(
        UUID schoolId,
        UUID policyId,
        String keyword,
        Boolean isActive,
        int page,
        int size
) {
}