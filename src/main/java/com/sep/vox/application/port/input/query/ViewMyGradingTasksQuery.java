package com.sep.vox.application.port.input.query;

/**
 * Hàng đợi giáo viên — MỘT danh sách cho cả bốn vòng, lọc thêm được theo
 * {@code roundType} nếu họ muốn tách ra xem riêng. Phân trang 0-based.
 */
public record ViewMyGradingTasksQuery(
    String status,
    String roundType,
    int page,
    int size
) {
}
