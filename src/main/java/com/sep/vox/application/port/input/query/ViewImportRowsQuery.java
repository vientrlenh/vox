package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewImportRowsQuery(
    UUID sessionId,
    int page,
    int size,
    String status
) {
}
