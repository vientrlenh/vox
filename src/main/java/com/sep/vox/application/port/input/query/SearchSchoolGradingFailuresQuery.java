package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Lọc danh sách bài AI chấm lỗi chưa ai xử lý của trường đang đăng nhập.
 *
 * <p>Không có {@code schoolId}: phạm vi lấy từ người đang đăng nhập. Nhận nó từ client là mở một
 * đường cho quản trị trường này đọc bài của trường khác.
 *
 * @param retryLeft true = chỉ bài còn lượt nhờ AI chấm lại, false = chỉ bài đã hết lượt, null =
 *                  không lọc. Ba trạng thái chứ không phải cờ bật/tắt, vì "không lọc" là mặc định
 *                  và khác hẳn "chỉ lấy bài hết lượt".
 */
public record SearchSchoolGradingFailuresQuery(
    UUID examId,
    Boolean retryLeft,
    int page,
    int size
) {
}
