package com.sep.vox.application.port.input.query;

import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportRowStatus;

public record ViewImportRowsQuery(
    UUID sessionId,
    int page,
    int size,
    ImportRowStatus status
) {
}
