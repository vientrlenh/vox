package com.sep.vox.application.port.input.query;

import java.util.UUID;

// Truy vấn danh sách Scoring Rule của 1 Assessment Policy.
// isActive = null nghĩa là không lọc theo trạng thái bật/tắt (lấy cả 2).
public record SearchScoringRuleQuery(
        UUID policyId,
        String keyword,
        Boolean isActive,
        int page,
        int size
) {
}