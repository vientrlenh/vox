package com.sep.vox.application.response.input.importfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreviewRubricResultBandImportResponse(
        UUID sessionId,                          // ID of the import session just created
        String fileName,                         // Name of the Excel/CSV file
        List<String> originalHeaders,            // The raw column headers read from the file
        Map<String, String> suggestedMapping,    // Suggested column mapping (Excel column -> system field)
        List<Map<String, String>> sampleRows,    // Preview of the first few rows (so the UI can show the preview table)
        long totalRows,                          // Total number of data rows read
        Instant expiresAt                 // Expiry time of this session
) {
}