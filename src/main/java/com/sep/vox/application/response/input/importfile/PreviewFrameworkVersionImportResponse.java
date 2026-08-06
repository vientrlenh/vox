package com.sep.vox.application.response.input.importfile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreviewFrameworkVersionImportResponse(
        UUID importSessionId,
        String fileName,
        List<String> originalHeaders,
        Map<String, String> suggestedMapping,
        List<Map<String, String>> sampleRows,
        long totalRows,
        Instant expiresAt
) {}
