package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Màn chấm của giáo viên. KHÔNG có tên/ID học sinh — giáo viên chấm ẩn danh.
 */
public record GradingTaskDetailInfo(
    UUID assignmentId,
    UUID candidateResultId,
    String resultCode,
    String examName,
    String assignmentStatus,
    String resultStatus,
    boolean flagged,
    String flagReason,
    BigDecimal currentTotalScore,
    // Cờ chỉ-đọc do BE quyết: chỉ true khi result còn PENDING_REVIEW và phân công
    // chưa COMPLETED. FE dùng cờ này, không tự suy từ status.
    boolean editable,
    List<GradingTaskItemInfo> items,
    List<GradingCriterionMetaInfo> criteria
) {
}
