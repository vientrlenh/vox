package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record RubricCriteriaKey(
        UUID versionId,
        int page,
        int size
) {}