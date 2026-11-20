package com.sep.vox.application.response.input.assessmentgrading;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The result of a trial grading. It must match EXACTLY the total that {@code /grade} returns when submitting
 * the same set of scores -- both go through {@code AssessmentSessionResultCalculator}.
 */
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
