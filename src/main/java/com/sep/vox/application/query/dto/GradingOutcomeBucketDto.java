package com.sep.vox.application.query.dto;

import java.time.LocalDate;

/**
 * Kết quả chấm AI của MỘT ngày lịch, gộp sẵn hai chiều thành công / thất bại.
 *
 * <p>Ngày ở đây là ngày lịch theo giờ Việt Nam ({@code ZoneConstant.BUSINESS_ZONE}), không phải ngày
 * UTC: cắt theo UTC đẩy 7 giờ đầu mỗi ngày sang ngày hôm trước, tức các ca thi buổi sáng sớm rơi
 * nhầm cột.
 */
public record GradingOutcomeBucketDto(
    LocalDate day,
    long graded,
    long failed
) {
}
