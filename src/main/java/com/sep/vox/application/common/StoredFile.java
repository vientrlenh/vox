package com.sep.vox.application.common;

public record StoredFile(
    String key,
    String url,
    String contentType,
    long size,
    String eTag
) {
}
