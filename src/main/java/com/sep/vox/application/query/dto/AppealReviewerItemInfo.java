package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Báo cáo chấm lại của một giám khảo cho một phần thi. */
public record AppealReviewerItemInfo(
    UUID appealItemId,
    String partLabel,
    BigDecimal suggestedScore,
    String note,
    List<AppealCriterionScoreInfo> scores
) {
}
