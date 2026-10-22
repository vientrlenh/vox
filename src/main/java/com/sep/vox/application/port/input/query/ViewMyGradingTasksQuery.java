package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.application.shared.AppPageRequest;

/**
 * Hàng đợi giáo viên — MỘT danh sách cho cả bốn vòng, lọc thêm được theo
 * {@code roundType} nếu họ muốn tách ra xem riêng. Trang đếm từ 1 ({@link AppPageRequest}).
 *
 * <p>{@code assessmentId} chỉ THU HẸP, không phải cổng phân quyền: phạm vi vẫn là các phân
 * công của chính người gọi, nên truyền id của kỳ thi mình không có bài vào chỉ ra danh
 * sách rỗng chứ không lộ gì.
 */
public record ViewMyGradingTasksQuery(
    UUID assessmentId,
    String status,
    String roundType,
    AppPageRequest pageRequest
) {
}
