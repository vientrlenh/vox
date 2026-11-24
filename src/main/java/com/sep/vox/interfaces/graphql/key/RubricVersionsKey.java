package com.sep.vox.interfaces.graphql.key;

import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricStatus;

public record RubricVersionsKey(
        UUID rubricId,
        RubricStatus status,
        int page,
        int size
) {}