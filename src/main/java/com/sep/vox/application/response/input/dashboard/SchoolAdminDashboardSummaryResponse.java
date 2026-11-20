package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record SchoolAdminDashboardSummaryResponse (
    ExamStatusCountResponse examStatusCounts,
    ExamAppealStatsResponse appealStats,
    BigDecimal revenue,
    List<SchoolMonthlySpendingResponse> monthlySpending,
    BigDecimal tokenAllocated,
    BigDecimal tokenUsed,
    SchoolSubscriptionRenewalResponse subscriptionRenewal,
    SchoolFundingResponse funding,
    UnscoredWorkloadResponse unscored,
    List<ExamAwaitingPublishResponse> examsAwaitingPublish,
    /** Số ngày đơn khiếu nại chờ lâu nhất đã chờ; null khi hàng đợi sạch. */
    Integer oldestPendingAppealDays
) {

}
