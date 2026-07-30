package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppealDetailInfo(
    UUID id,
    String studentName,
    String className,
    String examName,
    BigDecimal originalScore,
    String status,
    Instant requestedAt,
    Instant deadline,
    String reason,
    String notes,
    String decisionNote,
    BigDecimal finalScore,
    Instant approvedAt,
    Instant resolvedAt,
    /** Các phần thi được phúc khảo, mỗi phần kèm điểm AI gốc và lượt nói của riêng nó. */
    List<AppealItemInfo> items,
    List<AppealReviewerInfo> reviewers,
    boolean overdue,
    /** Thang điểm rubric — chính là khoảng BE dùng để validate partScore khi công bố. */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax
) {
}
