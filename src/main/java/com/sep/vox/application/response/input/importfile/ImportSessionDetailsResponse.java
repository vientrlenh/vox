package com.sep.vox.application.response.input.importfile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

public record ImportSessionDetailsResponse(
    UUID id,
    UUID schoolId,
    ImportType type,
    String fileName,
    List<String> originalHeaders,
    List<ImportMappingEntryResponse> suggestedMapping,
    List<ImportMappingEntryResponse> confirmedMapping,
    long validRows,
    long invalidRows,
    long importedRows,
    long skippedRows,
    long totalRows,
    String failureReason,
    ImportSessionStatus status,
    UUID importedEntityId,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {
}
