package com.sep.vox.interfaces.graphql.dto.request.rubricsystem;

import com.sep.vox.domain.model.rubric.RubricStatus;

public record SearchRubricVersionFilterRequest(
        String keyword,
        RubricStatus status
) {}