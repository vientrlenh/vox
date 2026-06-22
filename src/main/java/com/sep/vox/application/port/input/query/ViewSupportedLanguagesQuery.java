package com.sep.vox.application.port.input.query;

public record ViewSupportedLanguagesQuery(
    int page,
    int size,
    String search,
    Boolean isActive
) {
}
