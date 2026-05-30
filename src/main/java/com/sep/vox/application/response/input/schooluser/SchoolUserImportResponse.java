package com.sep.vox.application.response.input.schooluser;

import java.util.List;
import java.util.UUID;

public record SchoolUserImportResponse(
    String fileId,
    boolean dryRun,
    int totalRows,
    int processedRows,
    int createdCount,
    int failedCount,
    int skippedCount,
    List<SchoolUserImportError> errors,
    List<UUID> createdUserIds
) {
}
