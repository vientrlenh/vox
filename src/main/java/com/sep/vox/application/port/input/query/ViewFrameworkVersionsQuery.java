package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewFrameworkVersionsQuery(
    UUID frameworkId, 
    int page, 
    int size
) {
}
