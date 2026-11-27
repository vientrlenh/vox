package com.sep.vox.application.response.input.importfile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The result of the preview step of EVERY import flow: the session id, the original column titles, the suggested column mapping and a few
 * sample rows for the user to check before confirming.
 *
 * <p>Carries nothing specific to any one data type. {@code PreviewQuestionImportResponse} has identical
 * content but is kept as is, not merged -- changing the return type of a running endpoint is a risk
 * not worth taking for a naming cleanup.
 */
public record PreviewImportResponse(
    UUID importSessionId,
    String fileName,
    List<String> originalHeaders,
    Map<String, String> suggestedMapping,
    List<Map<String, String>> sampleRows,
    long totalRows,
    String expiresAt
) {
}
