package com.sep.vox.application.port.input.query;

import com.sep.vox.application.shared.AppPageRequest;

public record ViewImportSessionsQuery(
    AppPageRequest pageRequest,
    String type,
    String status
) {
}
