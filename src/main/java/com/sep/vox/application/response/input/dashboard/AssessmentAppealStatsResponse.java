package com.sep.vox.application.response.input.dashboard;

public record AssessmentAppealStatsResponse(
    long pending,
    long processing,
    long published,
    long rejected,
    long withdrawn
) {
    
}
