package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * Ứng viên cho picker phân công phúc khảo.
 *
 * <p>Người bị xung đột lợi ích KHÔNG bị lọc khỏi danh sách mà được đánh dấu
 * {@code conflicted}: admin cần thấy họ để hiểu vì sao lựa chọn hẹp, và ở trường nhỏ
 * còn phải override được — nhưng phải override một cách có ý thức.
 *
 * @param load số phân công đang mở người này đang giữ, gộp cả bốn vòng
 * @param conflicted đã từng ghi điểm tay cho chính bài này
 */
public record AppealReviewerLiteInfo(
    UUID id,
    String name,
    long load,
    boolean conflicted
) {
}
