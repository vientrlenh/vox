package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record AcceptAssessmentPolicyImportResponse(
        UUID sessionId,
        long totalRows,
        long importedRows,
        long invalidRows,
        long skippedRows,
        String status
) {}
