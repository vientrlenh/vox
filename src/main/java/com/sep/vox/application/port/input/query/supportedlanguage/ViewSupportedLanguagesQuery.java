package com.sep.vox.application.port.input.query.supportedlanguage;

public record ViewSupportedLanguagesQuery(
    int page,
    int size,
    String search,
    Boolean isActive
) {
}
