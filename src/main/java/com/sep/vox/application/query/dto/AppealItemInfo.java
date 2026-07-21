package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Một phần thi được phúc khảo, kèm dữ liệu gốc để đối chiếu.
 * `finalScore` null cho tới khi đơn được công bố.
 */
public record AppealItemInfo(
    UUID appealItemId,
    UUID paperItemId,
    String partLabel,
    List<AppealCriterionScoreInfo> aiScores,
    List<AppealTurnInfo> turns,
    BigDecimal finalScore
) {
}
