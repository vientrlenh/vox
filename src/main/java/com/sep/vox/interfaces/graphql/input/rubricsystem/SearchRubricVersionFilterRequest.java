package com.sep.vox.interfaces.graphql.input.rubricsystem;

import com.sep.vox.domain.model.rubric.RubricStatus;

public record SearchRubricVersionFilterRequest(
        String keyword,
        RubricStatus status
) {}