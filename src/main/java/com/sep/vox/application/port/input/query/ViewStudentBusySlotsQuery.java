package com.sep.vox.application.port.input.query;

import java.util.List;
import java.util.UUID;

/**
 * Trong nhóm {@code studentIds}, ai đang vướng lịch với khung giờ của từng ca trong
 * {@code scheduleIds}.
 *
 * <p>Số nhiều ở cả hai vế vì màn hình cần hỏi theo hai trục ngược nhau: "xếp MỘT học sinh vào ca
 * nào" (nhiều ca, một học sinh) và "thêm NHIỀU học sinh vào ca này" (một ca, nhiều học sinh). Một
 * lượt hỏi phục vụ được cả hai thay vì bắt giao diện gọi vòng.
 */
public record ViewStudentBusySlotsQuery(
    List<UUID> scheduleIds,
    List<UUID> studentIds
) {
}
