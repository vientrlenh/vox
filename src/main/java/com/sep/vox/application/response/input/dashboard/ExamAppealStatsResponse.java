package com.sep.vox.application.response.input.dashboard;

public record ExamAppealStatsResponse(
    long pending,
    long processing,
    long published,
    long rejected,
    long withdrawn
) {
    
}
