package com.sep.vox.application.port.input.query.rubricsystem;

import java.util.UUID;

public record SearchSystemRubricCriteriaQuery(
        UUID versionId,
        String keyword,
        Boolean isRequired,
        int page,
        int size
) {}