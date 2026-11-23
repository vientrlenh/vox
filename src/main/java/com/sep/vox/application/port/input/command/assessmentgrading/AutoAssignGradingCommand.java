package com.sep.vox.application.port.input.command.assessmentgrading;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.GradingSampleSelectionMode;

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
    UUID assessmentId,
    UUID scheduleId,
    GradingRoundType roundType,
    GradingSampleSelectionMode selectionMode,
    Integer percent,
    List<UUID> candidateResultIds,
    Instant deadlineAt,
    List<UUID> teacherIds
) {
}
