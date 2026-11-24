package com.sep.vox.application.port.input.query.rubricsystem;

import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricStatus;

public record SearchSystemRubricVersionsQuery(
        UUID rubricId,
        String keyword,
        RubricStatus status,
        int page,
        int size
) {}