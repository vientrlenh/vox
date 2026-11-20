package com.sep.vox.application.response.input.examschedule;

import java.util.UUID;

/**
 * Một giáo viên đang vướng lịch: đã gác ca {@code scheduleId} chạy từ {@code startDate} đến
 * {@code endDate}. Giao diện dùng để làm mờ giáo viên bận trong danh sách chọn kèm lý do, thay vì
 * để người dùng bấm rồi ăn lỗi.
 */
public record ProctorBusySlotResponse(
    UUID teacherId,
    UUID scheduleId,
    String startDate,
    String endDate
) {
}
