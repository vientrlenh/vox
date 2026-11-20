package com.sep.vox.interfaces.graphql.dto.request;

import com.sep.vox.domain.model.rubric.RubricStatus;

public record SearchRubricVersionFilterRequest(
        String keyword,
        RubricStatus status
) {}