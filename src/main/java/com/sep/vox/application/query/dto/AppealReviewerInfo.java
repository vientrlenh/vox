package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Người chấm phúc khảo của một đơn — chỉ MỘT người, và dữ liệu lấy từ dòng
 * {@code exam_grading_assignments} vòng {@code APPEAL} chứ không từ bảng riêng.
 *
 * <p>Không còn "điểm đề xuất": với một người chấm thì không có gì để đối chiếu, và
 * điểm họ nộp đi thẳng vào kết quả bài thi.
 */
public record AppealReviewerInfo(
    UUID assignmentId,
    UUID reviewerId,
    String reviewerName,
    /** ASSIGNED | COMPLETED */
    String status,
    /** UPHELD | REGRADED | DECLINED; null khi chưa nộp. */
    String outcome,
    OffsetDateTime assignedAt,
    OffsetDateTime completedAt,
    OffsetDateTime deadlineAt,
    boolean overdue
) {
}
