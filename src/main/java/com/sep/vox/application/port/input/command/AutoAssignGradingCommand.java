package com.sep.vox.application.port.input.command;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Giao tự động trong phạm vi một kỳ thi hoặc một ca thi, cho một vòng chấm.
 *
 * @param roundType     vòng cần giao; quyết định bài nào đủ điều kiện
 * @param selectionMode cách chọn bài trong tập đủ điều kiện (xem
 *                      {@code GradingSampleSelectionMode})
 * @param percent       chỉ dùng cho {@code RANDOM_PERCENT} và (tuỳ chọn)
 *                      {@code RISK_BASED}
 * @param candidateResultIds chỉ dùng cho {@code MANUAL_LIST}
 */
public record AutoAssignGradingCommand(
    UUID examId,
    UUID scheduleId,
    String roundType,
    String selectionMode,
    Integer percent,
    List<UUID> candidateResultIds,
    OffsetDateTime deadlineAt,
    List<UUID> teacherIds
) {
}
