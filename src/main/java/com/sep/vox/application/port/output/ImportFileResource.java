package com.sep.vox.application.port.output;

import java.io.InputStream;
import java.time.OffsetDateTime;

public record ImportFileResource(
    String fileId,
    String originalFileName,
    String format,
    long sizeBytes,
    OffsetDateTime expiresAt,
    InputStream inputStream
) {
}
