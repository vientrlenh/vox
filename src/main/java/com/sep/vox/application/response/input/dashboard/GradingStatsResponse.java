package com.sep.vox.application.response.input.dashboard;

public record GradingStatsResponse(
    long pending,
    long completed
) {
}
