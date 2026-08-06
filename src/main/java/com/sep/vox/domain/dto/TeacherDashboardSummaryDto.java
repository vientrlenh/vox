package com.sep.vox.domain.dto;

import java.math.BigDecimal;

import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.ExamStatusCountsDto;

public record TeacherDashboardSummaryDto(
    ExamStatusCountsDto examStatusCounts,
    GradingStatsDto gradingStats,
    ScoreStatsDto scoreStats
) {

    public record GradingStatsDto(
        long pending,
        long completed
    ) {

    }

    public record ScoreStatsDto(
        BigDecimal averageScore,
        long gradedCount,
        long totalCandidates
    ) {

    }

}
