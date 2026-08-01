package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewMyClassTestGradingTasksQuery(
    UUID examId,
    String status,
    String roundType,
    int page,
    int size
) {
}
