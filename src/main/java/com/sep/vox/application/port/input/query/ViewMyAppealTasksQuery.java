package com.sep.vox.application.port.input.query;

public record ViewMyAppealTasksQuery(
    String status,
    int page,
    int size
) {
}
