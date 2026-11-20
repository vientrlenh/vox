package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewFrameworkVersionsQuery(
    UUID frameworkId,
    String status,
    int page,
    int size
) {
}
