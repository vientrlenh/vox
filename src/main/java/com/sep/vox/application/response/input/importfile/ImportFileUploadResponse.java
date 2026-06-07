package com.sep.vox.application.response.input.importfile;

import java.time.OffsetDateTime;

public record ImportFileUploadResponse(
    String fileId,
    String originalFileName,
    String format,
    long sizeBytes,
    OffsetDateTime expiresAt
) {
}
