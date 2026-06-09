package com.sep.vox.application.response.input.importfile;

import java.util.List;
import java.util.UUID;

public record ImportSessionDetailsResponse(
    UUID id,
    UUID schoolId,
    String type,
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
    String status,
    UUID importedEntityId,
    String expiresAt,
    String createdAt,
    String updatedAt
) {
}
