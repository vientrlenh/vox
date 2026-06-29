package com.sep.vox.application.response.input.importfile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreviewRubricCriterionBandImportResponse(
        UUID sessionId,
        String fileName,
        List<String> originalHeaders,
        Map<String, String> suggestedMapping,
        List<Map<String, String>> sampleRows,
        long totalRows,
        OffsetDateTime expiresAt // Thời gian hết hạn của phiên làm việc này
){}