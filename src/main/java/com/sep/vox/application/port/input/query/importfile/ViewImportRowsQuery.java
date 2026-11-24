package com.sep.vox.application.port.input.query.importfile;

import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportRowStatus;

public record ViewImportRowsQuery(
    UUID sessionId,
    int page,
    int size,
    ImportRowStatus status
) {
}
