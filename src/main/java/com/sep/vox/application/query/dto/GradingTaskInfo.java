package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Hàng đợi của giáo viên — MỘT danh sách cho cả bốn vòng, phân biệt bằng
 * {@code roundType}.
 *
 * <p><strong>Ẩn danh là luật của kỳ thi {@code CENTRALIZED}</strong>, không phải luật
 * chung: ở đó {@code studentName}/{@code className} luôn {@code null} để bảo đảm chấm
 * mù. Bài kiểm tra trên lớp thì ngược lại — người chấm chính là giáo viên dạy lớp đó,
 * họ cần biết đang chấm ai. Xem {@code JpaExamGradingQueryRepository}.
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
    Instant assignedAt,
    Instant deadlineAt,
    boolean overdue,
    /** Chỉ có giá trị với bài kiểm tra trên lớp; kỳ thi tập trung luôn null. */
    String studentName,
    /** Chỉ có giá trị với bài kiểm tra trên lớp; kỳ thi tập trung luôn null. */
    String className
) {
}
