package com.sep.vox.application.response.input.schoolgrade;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreviewSchoolGradeImportResponse(
    UUID importSessionId,
    String fileName,
    List<String> originalHeaders,
    Map<String, String> suggestedMapping,
    List<Map<String, String>> sampleRows,
    long totalRows,
    String expiresAt
) {
}
