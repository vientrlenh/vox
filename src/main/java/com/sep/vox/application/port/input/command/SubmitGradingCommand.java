package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Giáo viên nộp điểm cho các phần thi của một bài. Cùng một command dùng cho cả
 * {@code /regrade} (ghi & chốt) lẫn {@code /regrade/preview} (chỉ tính thử) — hai
 * đường phải nhận đúng một đầu vào thì tổng chúng trả về mới bằng nhau.
 *
 * <p>KHÔNG có điểm tổng: tổng luôn được dẫn xuất lại từ điểm tiêu chí.
 *
 * <p>Vào bằng {@code assignmentId}, không phải {@code candidateResultId}: quyền chấm
 * đến từ chính dòng phân công, và vòng chấm ({@code roundType}) cũng nằm ở đó.
 */
public record SubmitGradingCommand(
    UUID assignmentId,
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
