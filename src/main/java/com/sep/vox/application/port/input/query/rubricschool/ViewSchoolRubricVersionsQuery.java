package com.sep.vox.application.port.input.query.rubricschool;

import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricStatus;

public record ViewSchoolRubricVersionsQuery(
        UUID schoolId,
        UUID rubricId,
        RubricStatus status,
        int page,
        int size
) {}