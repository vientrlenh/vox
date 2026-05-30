package com.sep.vox.application.response.input.schooluser;

import java.time.OffsetDateTime;

public record SchoolUserImportUploadResponse(
    String fileId,
    String originalFileName,
    String format,
    long sizeBytes,
    OffsetDateTime expiresAt
) {
}
