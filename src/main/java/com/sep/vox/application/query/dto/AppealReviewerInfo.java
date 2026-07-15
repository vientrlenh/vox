package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppealReviewerInfo(
    UUID reviewerId,
    String reviewerName,
    String status,
    boolean done,
    OffsetDateTime assignedAt,
    OffsetDateTime submittedAt,
    BigDecimal suggestedScore,
    String note,
    List<AppealCriterionScoreInfo> scores
) {
}
