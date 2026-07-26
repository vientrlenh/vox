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
    OffsetDateTime withdrawnAt,
    /** Lý do admin giao cho người đã từng chấm bài này; null khi không override. */
    String reviewerOverrideReason,
    /** Các phần thi được phúc khảo, mỗi phần kèm điểm gốc và lượt nói của riêng nó. */
    List<AppealItemInfo> items,
    /** Chỉ MỘT người chấm; null khi chưa phân công. */
    AppealReviewerInfo reviewer,
    boolean overdue,
    /** Thang điểm rubric — khoảng BE dùng để validate điểm tiêu chí khi chấm lại. */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax
) {
}
