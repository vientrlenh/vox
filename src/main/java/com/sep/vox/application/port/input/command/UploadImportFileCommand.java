package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UploadImportFileCommand(
    UUID schoolId,
    String originalFileName,
    String contentType,
    byte[] content
) {
}
