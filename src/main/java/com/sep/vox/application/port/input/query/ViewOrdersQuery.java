package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Bộ lọc danh sách đơn cho System Admin. Mọi trường lọc đều nullable = "không lọc theo tiêu chí này".
 *
 * <p>status/type nhận chuỗi thô để giữ nguyên hình dạng query string; use case tự parse và tự báo lỗi
 * đọc được thay vì để Spring ném ra một MethodArgumentTypeMismatchException khó hiểu khi FE gửi sai.
 */
public record ViewOrdersQuery(
    UUID schoolId,
    String status,
    String type,
    String keyword,
    int page,
    int size
) {
}
