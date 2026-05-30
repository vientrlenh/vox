package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UploadSchoolUserImportFileCommand(
    UUID schoolId,
    String originalFileName,
    String contentType,
    byte[] content
) {
}
