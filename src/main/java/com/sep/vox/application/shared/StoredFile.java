package com.sep.vox.application.shared;

public record StoredFile(
    String key,
    String url,
    String contentType,
    long size,
    String eTag
) {
}
