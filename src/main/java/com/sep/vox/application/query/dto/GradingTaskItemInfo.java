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
     * Đề bài của câu, lấy thẳng từ {@code questions.question_text} qua
     * {@code exam_paper_items.question_id} -- cùng nguồn với màn Xem kết quả
     * ({@code ViewExamSessionResultUseCase#itemResponses}).
     *
     * <p>Trước đây màn chấm KHÔNG có trường này: giáo viên chỉ thấy "Phần 1, câu 2" và phải
     * đoán đề bài qua {@code promptText} của lượt nói. Mà lời dẫn đó là câu AI đọc lúc vào lượt
     * ("You have 5 seconds to get ready…") chứ không phải đề, nên chấm mà không biết câu hỏi là
     * gì -- không đánh giá nổi câu trả lời có đúng trọng tâm hay không.
     *
     * <p>{@code null} khi paper item không trỏ tới câu hỏi nào hoặc câu hỏi đã bị xoá.
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
