package com.sep.vox.application.response.input.dashboard;

import java.util.List;

/**
 * Shares {@link AssessmentStatusCountResponse} with the School Admin dashboard -- the same counter, differing
 * only in the scope of assessments counted.
 */
public record TeacherDashboardSummaryResponse(
    AssessmentStatusCountResponse assessmentStatusCounts,
    GradingStatsResponse gradingStats,
    ScoreStatsResponse scoreStats,
    List<SchoolClassScoreStatsResponse> classScoreStats
) {


}
