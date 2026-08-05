package com.sep.vox.application.response.input.examitemresponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExamItemEvaluationDetailsResponse(
    UUID id,
    UUID responseId,
    UUID paperItemId,
    String engineType,
    String gradedByModel,
    String promptVersion,
    BigDecimal rawItemScore,
    BigDecimal itemScore,
    BigDecimal overallConfidence,
    boolean requiresHumanReview,
    String reviewReasonCode,
    boolean markedInvalid,
    boolean requiresRetake,
    String status,
    String evaluatedAt,
    String feedbackSummary,
    String signals,
    String validity,
    String suggestions,
    List<ExamItemCriterionScoreResponse> criteria,
    /**
     * Luôn lấy theo bản AI — chỉ bản AI mới sinh lượt nói. Bản chấm tay không có turn nào,
     * nên nếu đọc theo bản đang hiệu lực thì audio/transcript/nội dung câu hỏi biến mất
     * ngay khi giáo viên chấm lại.
     */
    List<ExamItemEvaluationTurnResponse> turns,
    /** Ngữ cảnh AI; {@code null} khi bài chưa từng có bản AI. */
    ExamItemAiEvaluationContextResponse ai
) {
}
