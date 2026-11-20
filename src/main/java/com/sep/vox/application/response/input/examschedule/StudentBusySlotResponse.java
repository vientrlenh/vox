package com.sep.vox.application.response.input.examschedule;

import java.util.UUID;

/**
 * Một học sinh đang vướng lịch khi cân nhắc xếp vào ca {@code targetScheduleId}: họ đã được xếp ca
 * {@code busyScheduleId} chạy từ {@code startDate} đến {@code endDate}.
 *
 * <p>Giao diện dùng để làm mờ sẵn lựa chọn bị trùng kèm lý do, thay vì để người dùng bấm rồi ăn lỗi.
 * Không kèm tên kỳ thi đang vướng: người xếp lịch kỳ này có thể không có quyền nhìn kỳ kia.
 */
public record StudentBusySlotResponse(
    UUID studentId,
    UUID targetScheduleId,
    UUID busyScheduleId,
    String startDate,
    String endDate
) {
}
