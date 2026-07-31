package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Dòng bảng phân công của school admin. Admin thấy tên học sinh bình thường —
 * ẩn danh chỉ áp cho phía giáo viên.
 *
 * <p>{@code assignmentId} null nghĩa là bài chưa được gán cho ai.
 */
public record GradingAssignmentRowInfo(
    UUID candidateResultId,
    String resultCode,
    String studentName,
    String className,
    String examName,
    String resultStatus,
    boolean flagged,
    UUID assignmentId,
    UUID teacherId,
    String teacherName,
    String assignmentStatus,
    Instant assignedAt,
    Instant completedAt
) {
}
