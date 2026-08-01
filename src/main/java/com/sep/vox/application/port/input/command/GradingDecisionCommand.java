package com.sep.vox.application.port.input.command;

import java.util.UUID;

/**
 * Đầu vào chung của ba hành động không nhập điểm: {@code UPHOLD}, {@code INVALIDATE},
 * {@code CLEAR_INVALID} và {@code DECLINE}.
 *
 * <p>Ba hành động đó chỉ khác nhau ở kết luận, không khác ở dữ liệu, nên dùng chung
 * một command. Riêng {@code REGRADE} có {@link SubmitGradingCommand} vì nó mang theo
 * cả bảng điểm.
 *
 * @param reason bắt buộc với {@code INVALIDATE} / {@code CLEAR_INVALID} / {@code DECLINE};
 *               tuỳ chọn với {@code UPHOLD} (ghi chú của giáo viên)
 */
public record GradingDecisionCommand(
    UUID assignmentId,
    String reason
) {
}
