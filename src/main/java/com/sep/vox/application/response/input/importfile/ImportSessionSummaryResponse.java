package com.sep.vox.application.response.input.importfile;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

public record ImportSessionSummaryResponse(
    UUID id,
    UUID schoolId,
    ImportType type,
    String fileName,
    long totalRows,
    long validRows,
    long invalidRows,
    long importedRows,
    long skippedRows,
    ImportSessionStatus status,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {
}
