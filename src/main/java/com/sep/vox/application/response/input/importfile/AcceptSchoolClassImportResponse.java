package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record AcceptSchoolClassImportResponse(
    UUID importSessionId,
    long totalRows,
    long importedRows,
    long updatedRows,
    long invalidRows,
    long skippedRows,
    String status
) {
}
