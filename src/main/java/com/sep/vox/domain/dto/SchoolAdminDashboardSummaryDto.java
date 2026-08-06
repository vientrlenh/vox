package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record SchoolAdminDashboardSummaryDto(
    ExamStatusCountsDto examStatusCounts,
    AppealStatsDto appealStats,
    BigDecimal revenue,
    List<MonthlySpendingDto> monthlySpending,
    long tokenAllocated,
    long tokenUsed
) {

    public record MonthlySpendingDto(
        String month,
        BigDecimal amount
    ) {

    }

    public record ExamStatusCountsDto(
        long total,
        long draft,
        long scheduled,
        long inProgress,
        long closed,
        long resultsPublished,
        long cancelled
    ) {

    }

    public record AppealStatsDto(
        long pending,
        long processing,
        long published,
        long rejected,
        long withdrawn
    ) {

    }

}
