package com.sep.vox.application.response.input.schooldirectory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PreviewSchoolDirectoryImportResponse(
    UUID importSessionId,
    String fileName,
    List<String> originalHeaders,
    Map<String, String> suggestedMapping,
    List<Map<String, String>> sampleRows,
    long totalRows,
    String expiresAt
) {
}
