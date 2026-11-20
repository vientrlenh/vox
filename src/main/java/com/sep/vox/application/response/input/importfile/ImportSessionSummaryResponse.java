package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record ImportSessionSummaryResponse(
    UUID id,
    UUID schoolId,
    String type,
    String fileName,
    long totalRows,
    long validRows,
    long invalidRows,
    long importedRows,
    long skippedRows,
    String status,
    String expiresAt,
    String createdAt,
    String updatedAt
) {
}
