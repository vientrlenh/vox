package com.sep.vox.application.response.input.assessmentgrading;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GradingPreviewResponse(
    BigDecimal totalScore,
    String resultBandName,
    List<ItemScore> itemScores
) {
    public record ItemScore(
        UUID paperItemId,
        BigDecimal itemScore
    ) {
    }
}
