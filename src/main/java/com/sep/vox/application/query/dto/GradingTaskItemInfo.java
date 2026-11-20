package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.QuestionAssetDto;

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
    /**
     * The prompt of the question, taken straight from {@code questions.question_text} through
     * {@code assessment_paper_items.question_id} -- the same source as the Result screen
     * ({@code ViewAssessmentSessionResultUseCase#itemResponses}).
     *
     * <p>Previously the grading screen did NOT have this field: the teacher only saw "Part 1, question 2" and had to
     * guess the prompt through the {@code promptText} of the spoken turn. But that text is the line the AI reads when entering the turn
     * ("You have 5 seconds to get ready...") and not the prompt, so grading without knowing what the question
     * is -- one cannot judge whether the answer is on target.
     *
     * <p>{@code null} when the paper item does not point to any question or the question has been deleted.
     */
    String questionText,
    /**
     * Tài nguyên đi kèm câu hỏi (ảnh / audio / video / đoạn văn), {@code null} khi câu không có.
     *
     * <p>Thiếu trường này thì giáo viên chấm một câu tả tranh chỉ thấy đề bài
     * "Describe the picture." cùng transcript của thí sinh, mà KHÔNG có tấm ảnh — không có cách
     * nào biết thí sinh tả đúng hay bịa. Với câu nghe còn nặng hơn: không nghe được đoạn thí sinh
     * đã nghe thì không chấm nổi phần nội dung.
     *
     * <p>Quan trọng hơn kể từ khi AI chỉ biết asset qua {@code transcript}/{@code description} do
     * người soạn gõ: con người là lớp kiểm tra cuối cho đúng loại sai sót đó, mà lớp đó đang bị
     * bịt mắt. Đây cũng là chỗ duy nhất phát hiện được ca asset hỏng không phát được ở máy thí sinh.
     */
    QuestionAssetDto asset,
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
