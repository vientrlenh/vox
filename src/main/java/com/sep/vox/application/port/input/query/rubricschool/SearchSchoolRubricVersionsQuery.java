package com.sep.vox.application.port.input.query.rubricschool;

import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricStatus;

public record SearchSchoolRubricVersionsQuery(
        UUID schoolId,
        UUID rubricId,
        String keyword,
        RubricStatus status,
        int page,
        int size
) {}