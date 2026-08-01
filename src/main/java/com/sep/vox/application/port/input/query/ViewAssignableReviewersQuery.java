package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Ứng viên chấm phúc khảo cho MỘT đơn.
 *
 * <p>Cần {@code appealId} vì cờ xung đột lợi ích tính theo bài thi của đơn đó, không
 * phải theo trường.
 */
public record ViewAssignableReviewersQuery(
    UUID appealId,
    String keyword
) {
}
