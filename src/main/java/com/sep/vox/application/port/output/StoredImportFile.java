package com.sep.vox.application.port.output;

import java.time.OffsetDateTime;

public record StoredImportFile(
    String fileId,
    String originalFileName,
    String format,
    long sizeBytes,
    OffsetDateTime expiresAt
) {
}
