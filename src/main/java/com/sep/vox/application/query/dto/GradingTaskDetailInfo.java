package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Màn chấm của giáo viên.
 *
 * <p>Với kỳ thi {@code CENTRALIZED}, {@code studentName}/{@code className} luôn
 * {@code null} — chấm mù là bảo đảm công bằng. Bài kiểm tra trên lớp thì có, vì
 * người chấm chính là giáo viên dạy lớp đó.
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
    List<GradingCriterionMetaInfo> criteria,
    /** Chỉ có giá trị với bài kiểm tra trên lớp; kỳ thi tập trung luôn null. */
    String studentName,
    /** Chỉ có giá trị với bài kiểm tra trên lớp; kỳ thi tập trung luôn null. */
    String className,
    /** Phiên thi đang chấm. Hai lượt của cùng một em khác nhau ở đây. */
    UUID sessionId,
    /** Lượt thi thứ mấy, đếm từ 1 theo thời điểm bắt đầu làm bài. */
    int attemptNo,
    /** Tổng số lượt em đó đã làm ở bài thi này. Bằng 1 nghĩa là không có thi lại. */
    int attemptCount
) {
}
