package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

/** Hàng đợi của giáo viên. Ẩn danh: không có tên/ID học sinh. */
public record GradingTaskInfo(
    UUID assignmentId,
    UUID candidateResultId,
    String resultCode,
    String examName,
    int partCount,
    String status,
    boolean flagged,
    Instant assignedAt
) {
}
