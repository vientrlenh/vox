package com.sep.vox.interfaces.graphql.dto.request.rubricsystem;

public record SearchRubricCriterionFilterRequest(
        String keyword,
        Boolean isRequired
) {}