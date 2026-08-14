package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record SchoolAdminDashboardSummaryDto(
    ExamStatusCountsDto examStatusCounts,
    AppealStatsDto appealStats,
    BigDecimal revenue,
    List<MonthlySpendingDto> monthlySpending,
    BigDecimal tokenAllocated,
    BigDecimal tokenUsed,
    SubscriptionRenewalDto subscriptionRenewal
) {

    /** {@code null} nếu trường chưa từng có gói đăng ký nào đang hoạt động. */
    public record SubscriptionRenewalDto(
        String planName,
        String status,
        String endDate
    ) {

    }

    public record MonthlySpendingDto(
        String month,
        BigDecimal amount,
        BigDecimal subscriptionAmount,
        BigDecimal tokenTopUpAmount
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
