package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Màn chấm của giáo viên. KHÔNG có tên/ID học sinh — giáo viên chấm ẩn danh.
 *
 * <p>{@code allowedOutcomes} do BE quyết theo {@code GradingRoundPolicy}: FE dựng
 * đúng những nút được phép của vòng này, không tự suy từ {@code roundType} — nếu suy
 * ở hai nơi thì sớm muộn hai nơi lệch nhau.
 */
public record GradingTaskDetailInfo(
    UUID assignmentId,
    UUID candidateResultId,
    String resultCode,
    String examName,
    String roundType,
    String assignmentStatus,
    String resultStatus,
    boolean flagged,
    String flagReason,
    BigDecimal currentTotalScore,
    /** Điểm lúc được giao — mốc để giáo viên biết mình đang sửa từ đâu. */
    BigDecimal scoreBefore,
    Instant deadlineAt,
    boolean overdue,
    /** Cờ chỉ-đọc do BE quyết: chỉ true khi phân công còn ASSIGNED và bài đúng vòng. */
    boolean editable,
    List<String> allowedOutcomes,
    /** Chỉ có ở vòng APPEAL: lý do học sinh nêu trong đơn. */
    String appealReason,
    List<GradingTaskItemInfo> items,
    List<GradingCriterionMetaInfo> criteria
) {
}
