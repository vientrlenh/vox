package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record SearchSchoolRubricCriteriaQuery(
        UUID schoolId,
        UUID versionId,
        String keyword,
        Boolean isRequired,
        int page,
        int size
) {}