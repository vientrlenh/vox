package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record ViewSystemRubricCriteriaQuery(
        UUID versionId,
        int page,
        int size
) {}