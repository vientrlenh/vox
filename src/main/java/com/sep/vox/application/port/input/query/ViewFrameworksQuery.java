package com.sep.vox.application.port.input.query;

public record ViewFrameworksQuery(
    int page,
    int size,
    String search,
    Boolean isActive
) {
}
