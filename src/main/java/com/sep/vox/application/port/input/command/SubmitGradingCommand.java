package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Giáo viên nộp điểm cho các phần thi của một bài. Cùng một command dùng cho cả
 * {@code /grade} (ghi & chốt) lẫn {@code /grade/preview} (chỉ tính thử) — hai
 * đường phải nhận đúng một đầu vào thì tổng chúng trả về mới bằng nhau.
 *
 * <p>KHÔNG có điểm tổng: tổng luôn được dẫn xuất lại từ điểm tiêu chí.
 *
 * <p>Đúng một trong {@code assignmentId}/{@code candidateResultId} khác null:
 * {@code assignmentId} cho luồng giáo viên đã được phân công, {@code candidateResultId}
 * cho luồng nhà trường chấm trực tiếp một bài PENDING_REVIEW chưa (hoặc đã) có phân công.
 */
public record SubmitGradingCommand(
    UUID assignmentId,
    UUID candidateResultId,
    List<ItemGrade> items
) {
    public record ItemGrade(
        UUID paperItemId,
        List<CriterionScoreItem> criterionScores,
        String feedbackSummary
    ) {
    }

    public record CriterionScoreItem(
        UUID rubricCriterionId,
        BigDecimal score,
        String rationale
    ) {
    }
}
