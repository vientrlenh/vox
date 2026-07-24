package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Một phần thi cần chấm: lượt nói để nghe + điểm đang có hiệu lực để tham chiếu. */
public record GradingTaskItemInfo(
    UUID paperItemId,
    UUID responseId,
    String partLabel,
    BigDecimal currentItemScore,
    String currentFeedbackSummary,
    List<GradingCriterionScoreInfo> currentScores,
    List<GradingTurnInfo> turns
) {
}
