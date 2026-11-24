package com.sep.vox.application.port.input.query.importfile;

import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

public record ViewImportSessionsQuery(
    int page,
    int size,
    ImportType type,
    ImportSessionStatus status
) {
}
