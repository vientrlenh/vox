package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

// Truy vấn danh sách Scoring Rule của 1 Assessment Policy thuộc trường học.
public record SearchSchoolScoringRuleQuery(
        UUID schoolId,
        UUID policyId,
        String keyword,
        Boolean isActive,
        AppPageRequest pageRequest
) {
}