package com.sep.vox.application.port.input.query.rubricsystem;

import java.util.UUID;

public record ViewSystemRubricCriteriaQuery(
        UUID versionId,
        int page,
        int size
) {}