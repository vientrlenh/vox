package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * MỘT CÂU HỎI cần chấm: lượt nói để nghe, điểm đang có hiệu lực để tham chiếu, và
 * bằng chứng của bản AI để giáo viên chấm trên cùng dữ liệu mà AI đã dùng.
 *
 * <p>{@code partLabel} là tiêu đề section nên nhiều câu dùng chung một nhãn —
 * {@code sectionId} + {@code orderInSection} là thứ phân biệt chúng.
 *
 * <p>Các trường {@code ai*} lấy từ bản AI, KHÔNG phải bản đang hiệu lực: sau khi chấm
 * tay, bản hiệu lực là bản HUMAN (không có turn/signals/rationale của AI) nhưng vòng
 * phúc khảo vẫn phải đọc được bằng chứng gốc. Bài chưa có bản AI thì để null/false/rỗng.
 */
public record GradingTaskItemInfo(
    UUID paperItemId,
    UUID responseId,
    String partLabel,
    UUID sectionId,
    int orderInSection,
    BigDecimal currentItemScore,
    String currentFeedbackSummary,
    List<GradingCriterionScoreInfo> currentScores,
    List<GradingTurnInfo> turns,
    List<GradingCriterionScoreInfo> aiScores,
    BigDecimal aiOverallConfidence,
    String aiFeedbackSummary,
    boolean aiRequiresHumanReview,
    String aiReviewReasonCode,
    boolean aiMarkedInvalid,
    boolean aiRequiresRetake,
    String aiSignals,
    String aiValidity
) {
}
