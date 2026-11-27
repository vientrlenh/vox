package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record SchoolAdminDashboardSummaryResponse (
    AssessmentStatusCountResponse assessmentStatusCounts,
    AssessmentAppealStatsResponse appealStats,
    BigDecimal revenue,
    List<SchoolMonthlySpendingResponse> monthlySpending,
    BigDecimal tokenAllocated,
    BigDecimal tokenUsed,
    SchoolSubscriptionRenewalResponse subscriptionRenewal,
    SchoolFundingResponse funding,
    UnscoredWorkloadResponse unscored,
    List<AssessmentAwaitingPublishResponse> assessmentsAwaitingPublish,
    /** The number of days the longest-waiting complaint has waited; null when the queue is clean. */
    Integer oldestPendingAppealDays
) {

}
