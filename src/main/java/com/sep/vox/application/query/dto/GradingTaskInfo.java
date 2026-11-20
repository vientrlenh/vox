package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The teacher's queue -- ONE list for all four rounds, told apart by
 * {@code roundType}.
 *
 * <p><strong>Anonymity is a rule of {@code CENTRALIZED} assessments</strong>, not a general
 * rule: there {@code studentName}/{@code className} are always {@code null} to guarantee blind
 * grading. A class test is the opposite -- the grader is the very teacher who teaches that class,
 * and they need to know whom they are grading. See {@code JpaAssessmentGradingQueryRepository}.
 */
public record GradingTaskInfo(
    UUID assignmentId,
    UUID candidateResultId,
    String resultCode,
    String assessmentName,
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
    String className,
    /** Phiên thi sinh ra bài này. Hai lượt của cùng một em khác nhau ở đây. */
    UUID sessionId,
    /** Lượt thi thứ mấy, đếm từ 1 theo thời điểm bắt đầu làm bài. */
    int attemptNo,
    /** Tổng số lượt em đó đã làm ở bài thi này. Bằng 1 nghĩa là không có thi lại. */
    int attemptCount
) {
}
