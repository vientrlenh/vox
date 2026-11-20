package com.sep.vox.application.response.input.examitemresponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bằng chứng của bản chấm AI, giữ lại kể cả khi giáo viên đã chấm lại (lúc đó bản AI mang
 * SUPERSEDED). Điểm và rationale hiển thị vẫn là của bản đang có hiệu lực ở cấp trên —
 * khối này chỉ là NGỮ CẢNH.
 *
 * <p>Tách riêng thay vì điền bù vào chính field của bản chấm tay: một
 * {@code overallConfidence} gắn lên điểm do người chấm là vô nghĩa, và trộn lẫn thì người
 * đọc payload không còn cách nào biết con số đến từ bản nào. Cùng quy ước với
 * {@code GradingTaskItemInfo} ở màn chấm của giáo viên.
 */
public record ExamItemAiEvaluationContextResponse(
    UUID evaluationId,
    String engineType,
    String gradedByModel,
    String promptVersion,
    BigDecimal overallConfidence,
    boolean requiresHumanReview,
    String reviewReasonCode,
    boolean markedInvalid,
    boolean requiresRetake,
    String evaluatedAt,
    String feedbackSummary,
    String signals,
    String validity,
    String suggestions
) {
}
