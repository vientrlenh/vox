package com.sep.vox.application.port.input.query;

public record ViewImportSessionsQuery(
    int page,
    int size,
    String type,
    String status
) {
}
