package com.sep.vox.application.event;

import java.util.UUID;

/**
 * Giáo viên trả lại phân công. Người nhận tin là <em>admin đã giao</em>, không phải
 * học sinh: bài quay về hàng chưa giao và cần người điều phối xử lý tiếp.
 */
public record GradingAssignmentDeclinedEvent(
    UUID assignmentId,
    UUID candidateResultId,
    UUID teacherId,
    UUID assignedBy,
    String examName,
    String reason
) {
}
