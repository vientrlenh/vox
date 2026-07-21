package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppealDetailInfo(
    UUID id,
    String studentName,
    String className,
    String examName,
    String partLabel,
    BigDecimal originalScore,
    String status,
    OffsetDateTime requestedAt,
    OffsetDateTime deadline,
    String reason,
    String notes,
    String decisionNote,
    BigDecimal finalScore,
    OffsetDateTime approvedAt,
    OffsetDateTime resolvedAt,
    List<AppealCriterionScoreInfo> aiScores,
    List<AppealTurnInfo> turns,
    List<AppealReviewerInfo> reviewers,
    boolean overdue,
    /** Thang điểm rubric — chính là khoảng BE dùng để validate partScore khi công bố. */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax
) {
}
