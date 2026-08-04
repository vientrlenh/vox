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
 *
 * <p>Một dòng là MỘT LƯỢT THI, không phải một học sinh: em nào thi lại thì có bấy nhiêu
 * dòng cùng tên. {@code attemptNo}/{@code attemptCount} là thứ duy nhất phân biệt được
 * chúng trên màn hình — không có nó thì người chấm nhìn hai dòng y hệt nhau.
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
    boolean hasOpenAppeal,
    /** Phiên thi sinh ra bài này. Hai lượt của cùng một em khác nhau ở đây. */
    UUID sessionId,
    /** Lượt thi thứ mấy, đếm từ 1 theo thời điểm bắt đầu làm bài. */
    int attemptNo,
    /** Tổng số lượt em đó đã làm ở bài thi này. Bằng 1 nghĩa là không có thi lại. */
    int attemptCount
) {
}
