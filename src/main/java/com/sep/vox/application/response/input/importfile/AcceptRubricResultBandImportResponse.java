package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record AcceptRubricResultBandImportResponse(
        UUID sessionId,         // ID of the import session just pushed onto the background queue
        long totalRows,         // Total number of data rows (so the UI can show a progress bar if needed)
        long importedRows,      // Number of rows imported successfully (0 by default when QUEUED)
        long invalidRows,       // Number of rows with errors (0 by default when QUEUED)
        long skippedRows,       // Number of skipped rows (0 by default when QUEUED)
        String status           // Current status of the import session (will be "QUEUED")
) {
}