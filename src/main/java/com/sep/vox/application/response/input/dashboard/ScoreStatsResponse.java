package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

public record ScoreStatsResponse(
    BigDecimal averageScore,
    long gradedCount,
    long totalCandidates
) {
    
}
