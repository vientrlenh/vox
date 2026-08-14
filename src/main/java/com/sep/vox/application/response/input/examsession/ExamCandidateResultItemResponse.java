package com.sep.vox.application.response.input.examsession;

import java.math.BigDecimal;
import java.util.UUID;

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
    BigDecimal itemScore,
    BigDecimal weightedScore
) {
}
