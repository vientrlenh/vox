package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

public record SchoolClassScoreStatsResponse(
    String examName,
    String className,
    BigDecimal averageScore,
    BigDecimal highestScore,
    BigDecimal lowestScore,
    long gradedCount,
    long totalCandidates
) {
    
}
