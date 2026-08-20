package com.sep.vox.application.response.input.examsession;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.dto.QuestionAssetDto;

/**
 * @param questionText đề bài của câu này, lấy thẳng từ {@code questions.question_text}.
 *                     Trước đây không có field này nên trang kết quả học sinh phải suy ra nội
 *                     dung câu hỏi từ {@code turns[].promptText} -- vừa đòi mở thẻ ra mới nạp
 *                     (đóng thì hiện "Không có nội dung câu hỏi." dù dữ liệu vẫn còn nguyên),
 *                     vừa lấy nhầm lời dẫn của AI ("You have 10 seconds to prepare...") thay vì
 *                     đề bài. Null khi paper item không trỏ tới câu hỏi nào, hoặc câu hỏi đã bị
 *                     xoá khỏi ngân hàng.
 */
public record ExamCandidateResultItemResponse(
    UUID paperItemId,
    UUID responseId,
    UUID sectionId,
    String questionText,
    /**
     * Tài nguyên đi kèm câu hỏi, {@code null} khi câu không có.
     *
     * <p>Cùng lý do với màn chấm: đọc lại bài mà không thấy tấm ảnh / không nghe lại được đoạn
     * nghe thì không đối chiếu nổi câu trả lời với đề. Hiện cho CẢ giáo viên lẫn học sinh —
     * trang này vốn đã trả {@code questionText}, nên asset không làm lộ thêm gì, và thí sinh
     * thì vừa nhìn tấm ảnh đó xong trong phòng thi.
     */
    QuestionAssetDto asset,
    BigDecimal itemScore,
    BigDecimal weightedScore
) {
}
