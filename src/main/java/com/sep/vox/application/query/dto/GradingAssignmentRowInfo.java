package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Dòng bảng phân công của school admin. Admin thấy tên học sinh bình thường —
 * ẩn danh chỉ áp cho phía giáo viên.
 *
 * <p>{@code assignmentId} null nghĩa là bài chưa có phân công nào đang mở. Bài có
 * thể đã qua vài vòng trước đó; những vòng đã đóng nằm ở màn lịch sử, không chen vào
 * bảng điều phối này.
 */
public record GradingAssignmentRowInfo(
    UUID candidateResultId,
    String resultCode,
    String studentName,
    String className,
    String examName,
    String resultStatus,
    BigDecimal totalScore,
    boolean flagged,
    UUID assignmentId,
    UUID teacherId,
    String teacherName,
    String roundType,
    String assignmentStatus,
    String outcome,
    Instant assignedAt,
    Instant completedAt,
    Instant deadlineAt,
    boolean overdue,
    /** Còn đơn phúc khảo chưa kết thúc — admin cần biết trước khi giao vòng khác. */
    boolean hasOpenAppeal
) {
}
