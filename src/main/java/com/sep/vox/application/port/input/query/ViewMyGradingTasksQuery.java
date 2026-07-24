package com.sep.vox.application.port.input.query;

/** Phân trang 0-based. */
public record ViewMyGradingTasksQuery(
    String status,
    int page,
    int size
) {
}
