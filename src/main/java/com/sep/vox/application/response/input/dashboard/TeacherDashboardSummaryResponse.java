package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * Dùng chung {@link ExamStatusCountResponse} với dashboard của School Admin -- cùng một bộ đếm, chỉ
 * khác phạm vi kỳ thi được đếm.
 */
public record TeacherDashboardSummaryResponse(
    ExamStatusCountResponse examStatusCounts,
    GradingStatsResponse gradingStats,
    ScoreStatsResponse scoreStats,
    List<SchoolClassScoreStatsResponse> classScoreStats
) {


}
