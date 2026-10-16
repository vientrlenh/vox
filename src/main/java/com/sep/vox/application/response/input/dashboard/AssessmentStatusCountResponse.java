package com.sep.vox.application.response.input.dashboard;

public record AssessmentStatusCountResponse(
    long total,
    long draft,
    long scheduled,
    long inProgress,
    long closed,
    long resultsPublished,
    long cancelled
) {
    
}
