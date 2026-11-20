package com.sep.vox.application.port.input.query;

import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricStatus;

public record ViewSystemRubricVersionsQuery(
        UUID rubricId,
        RubricStatus status,
        int page,
        int size
) {}