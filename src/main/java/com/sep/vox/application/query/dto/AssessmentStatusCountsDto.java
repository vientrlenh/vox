package com.sep.vox.application.query.dto;

public record AssessmentStatusCountsDto(
    long total,
    long draft,
    long scheduled,
    long inProgress,
    long closed,
    long resultsPublished,
    long cancelled
) {
}
