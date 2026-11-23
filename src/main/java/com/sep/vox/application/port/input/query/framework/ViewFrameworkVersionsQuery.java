package com.sep.vox.application.port.input.query.framework;

import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public record ViewFrameworkVersionsQuery(
    UUID frameworkId,
    FrameworkVersionStatus status,
    int page,
    int size
) {
}
