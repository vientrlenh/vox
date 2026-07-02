package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record AcceptRubricCriterionBandImportResponse(
        UUID sessionId,
        long totalRows,
        long importedRows,
        long invalidRows,
        long skippedRows,
        String status           // Trạng thái hiện tại của phiên làm việc (Chắc chắn sẽ là "QUEUED")
) {
}