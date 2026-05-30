package com.sep.vox.application.port.output;

public record ImportFileData(
    String originalFileName,
    String contentType,
    byte[] content
) {
}
