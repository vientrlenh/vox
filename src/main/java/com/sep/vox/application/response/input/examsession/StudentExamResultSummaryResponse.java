package com.sep.vox.application.response.input.examsession;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentExamResultSummaryResponse(
    UUID candidateId,
    UUID examId,
    String examCode,
    String examName,
    String kind,
    UUID sessionId,
    UUID paperId,
    String sessionStatus,
    boolean sessionFlagged,
    String startedAt,
    String submittedAt,
    BigDecimal totalScore,
    /**
     * Thang điểm của rubric đã chấm lượt này -- thêm 2026-08-11, cùng lý do với
     * {@code ExamAttemptSummary.scoringScaleMin}: danh sách này trộn nhiều kỳ thi, mỗi kỳ có thể
     * dùng rubric khác thang, nên client không suy ra được từ đâu ngoài chính dòng đó.
     *
     * <p>Null khi điểm đang bị che -- lúc đó không có số nào để vẽ nên thang cũng vô nghĩa.
     */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax,
    String resultStatus,
    UUID rubricResultBandId,
    String rubricResultBandCode,
    String rubricResultBandName
) {
}
