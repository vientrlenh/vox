package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;

import com.sep.vox.domain.dto.SchoolAdminDashboardSummaryDto.ExamStatusCountsDto;

public record TeacherDashboardSummaryDto(
    ExamStatusCountsDto examStatusCounts,
    GradingStatsDto gradingStats,
    ScoreStatsDto scoreStats,
    List<ClassScoreStatsDto> classScoreStats
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

    public record ClassScoreStatsDto(
        String examName,
        String className,
        BigDecimal averageScore,
        BigDecimal highestScore,
        BigDecimal lowestScore,
        long gradedCount,
        long totalCandidates
    ) {

    }

}
