package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Giao MỘT người chấm phúc khảo cho một đơn.
 *
 * @param overrideReason lý do cố tình giao cho người đã từng ghi điểm tay cho bài này.
 *        Không truyền = giữ luật xung đột lợi ích và use case sẽ từ chối. Truyền =
 *        admin chấp nhận rủi ro (trường nhỏ không đủ giáo viên) và lý do đi vào audit.
 * @param deadlineAt hạn chấm riêng cho vòng phúc khảo; bỏ trống thì lấy hạn của đơn
 */
public record AssignExamAppealReviewerCommand(
    UUID appealId,
    UUID reviewerId,
    String overrideReason,
    Instant deadlineAt
) {
}
