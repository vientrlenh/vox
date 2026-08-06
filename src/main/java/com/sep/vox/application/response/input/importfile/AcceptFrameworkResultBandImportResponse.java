package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record AcceptFrameworkResultBandImportResponse(
        UUID sessionId,
        long totalRows,
        long importedRows,
        long invalidRows,
        long skippedRows,
        String status
) {}
