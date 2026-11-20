package com.sep.vox.application.response.input.examgrading;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kết quả chung của bốn hành động chấm bài.
 *
 * <p>Một kiểu trả về cho cả bốn để FE dùng đúng một đường xử lý sau khi bấm nút:
 * đọc {@code resultStatus} + {@code totalScore} rồi vẽ lại dòng, không cần rẽ nhánh
 * theo nút vừa bấm.
 *
 * @param nextAssignmentId phân công vừa được mở tiếp trong cùng transaction; hiện chỉ
 *        {@code CLEAR_INVALID} sinh ra (mở vòng {@code INITIAL} cho chính giáo viên đó),
 *        {@code null} với các hành động còn lại
 */
public record GradingActionResponse(
    UUID assignmentId,
    UUID candidateResultId,
    String outcome,
    String resultStatus,
    BigDecimal totalScore,
    UUID nextAssignmentId
) {
}
