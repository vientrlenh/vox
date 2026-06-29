package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record SearchSchoolRubricVersionsQuery(
        UUID schoolId,
        UUID rubricId,
        String keyword,
        String status,
        int page,
        int size
) {}