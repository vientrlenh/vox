package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Hàng đợi của giáo viên — MỘT danh sách cho cả bốn vòng, phân biệt bằng
 * {@code roundType}. Ẩn danh: không có tên/ID học sinh.
 */
public record GradingTaskInfo(
    UUID assignmentId,
    UUID candidateResultId,
    String resultCode,
    String examName,
    int partCount,
    String roundType,
    String status,
    String resultStatus,
    /** Điểm bài đang có — ở vòng hậu kiểm/phúc khảo đây là điểm đã công bố. */
    BigDecimal currentScore,
    boolean flagged,
    OffsetDateTime assignedAt,
    OffsetDateTime deadlineAt,
    boolean overdue
) {
}
