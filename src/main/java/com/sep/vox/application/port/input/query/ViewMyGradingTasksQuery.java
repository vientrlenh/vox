package com.sep.vox.application.port.input.query;

import java.util.UUID;

/**
 * Hàng đợi giáo viên — MỘT danh sách cho cả bốn vòng, lọc thêm được theo
 * {@code roundType} nếu họ muốn tách ra xem riêng. Phân trang 0-based.
 *
 * <p>{@code examId} chỉ THU HẸP, không phải cổng phân quyền: phạm vi vẫn là các phân
 * công của chính người gọi, nên truyền id của kỳ thi mình không có bài vào chỉ ra danh
 * sách rỗng chứ không lộ gì.
 */
public record ViewMyGradingTasksQuery(
    UUID examId,
    String status,
    String roundType,
    int page,
    int size
) {
}
